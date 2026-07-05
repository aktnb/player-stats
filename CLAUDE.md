# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

`player-stats` is a Paper/Folia (Bukkit-based) Minecraft server plugin written in Kotlin. It tracks per-player block-mining/placing counts and exposes them via a `/stats` command, persisting them to a local SQLite database.

## Build & Development Commands

- Build the plugin jar (shadow/fat jar with dependencies bundled): `./gradlew build`
- Build only the shadow jar: `./gradlew shadowJar`
- Output jars land in `build/libs/` — `player-stats-<version>-all.jar` is the fat jar to deploy to a server's `plugins/` folder; `player-stats-<version>.jar` is the thin jar.
- Clean build artifacts: `./gradlew clean`
- There is no test suite and no lint task configured in this repo.
- Kotlin toolchain is JVM 25 (`kotlin { jvmToolchain(25) }` in `build.gradle.kts`). Gradle wrapper is used for all builds (`./gradlew`, not a system-installed Gradle).
- `version` (used for the plugin jar name and injected into `plugin.yml`) comes from `gradle.properties`.

## Architecture

The plugin follows a buffer → periodic flush → SQLite pattern to avoid blocking the main server thread with per-event database writes:

1. **`StatsListener`** (`listener/StatsListener.kt`) listens for `BlockBreakEvent`/`BlockPlaceEvent` and records increments into an in-memory **`StatsBuffer`** (`stats/StatsBuffer.kt`). This buffer is a `synchronized`-guarded map of `UUID -> PlayerStatsDelta`, keeping counts that haven't yet been persisted.
2. A repeating async task (scheduled from `PlayerStats.onEnable`, every 5s) calls **`StatsFlushService.flush()`** (`stats/StatsFlushService.kt`), which drains the buffer and writes the accumulated deltas to SQLite via **`StatsRepository`**. `StatsFlushService` uses an `AtomicBoolean` to guarantee only one flush runs at a time; on a failed write, drained deltas are merged back into the buffer (`StatsBuffer.mergeBlock`) rather than lost, and retried on the next flush.
3. **`StatsRepository`** (`repository/StatsRepository.kt`) owns all SQL (schema creation, upsert-by-delta via `INSERT ... ON CONFLICT DO UPDATE`, lookup by UUID) against a `player_stats` table (`player_uuid`, `player_name`, `blocks_mined`, `blocks_placed`, `last_seen_at`). It depends on **`SQLiteProvider`** (`repository/SQLiteProvider.kt`), which owns the JDBC connection string/pragmas (WAL journal mode, foreign keys, busy timeout) for the plugin's `stats.db` file under the plugin's data folder.
4. **`StatsCommand`** (`command/StatsCommand.kt`) handles `/stats`: it reads from `StatsRepository` off the main thread and hops back onto the entity's own thread/region to send the reply — required for Folia compatibility (see below).

### Scheduler abstraction (Folia + Paper/Spigot compatibility)

Because Folia uses per-region threading instead of Bukkit's single main thread, all scheduling goes through the **`PluginScheduler`** interface (`scheduler/PluginScheduler.kt`) rather than calling `Bukkit.getScheduler()` or entity schedulers directly:

- **`BukkitPluginScheduler`** — implementation for traditional Paper/Spigot, using `Bukkit.getScheduler()`.
- **`PaperFoliaPluginScheduler`** — implementation for Folia. Since Folia-only scheduler APIs (`Server#getAsyncScheduler`, `Entity#getScheduler`) don't exist in the Paper API this project compiles against, this implementation calls them via **reflection**.
- **`SchedulerFactory`** picks the right implementation at runtime by probing for the presence of `Server#getAsyncScheduler` (see `ServerPlatform` for the lower-level Folia/Paper class-existence checks used elsewhere).

Any new code that needs to schedule work (async task, or getting back onto an entity's thread) must go through `PluginScheduler`, never call Bukkit/Folia scheduler APIs directly — otherwise it will break on one of the two platforms.

### Plugin entry point

`PlayerStats.kt` (the `JavaPlugin` subclass, at the package root — note there's a same-named but distinct `stats/PlayerStats.kt` data class for the actual stats record) wires all components together in `onEnable()` and flushes any buffered stats synchronously in `onDisable()` so data isn't lost on shutdown/reload.

`plugin.yml` declares the `stats` command and `api-version: '26.1'`; the `main` class and `version` (templated from `gradle.properties`) are injected via Gradle's `processResources` filtering (see `build.gradle.kts`).
