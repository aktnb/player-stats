# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

`player-stats` is a Paper/Folia (Bukkit-based) Minecraft server plugin written in Kotlin. It tracks per-player block-mining/placing counts and exposes them via a `/stats` command, reading Minecraft's own standard statistics (`Player.getStatistic`) on demand. The plugin itself holds no persistence layer.

## Build & Development Commands

- Build the plugin jar (shadow/fat jar with dependencies bundled): `./gradlew build`
- Build only the shadow jar: `./gradlew shadowJar`
- Output jars land in `build/libs/` — `player-stats-<version>-all.jar` is the fat jar to deploy to a server's `plugins/` folder; `player-stats-<version>.jar` is the thin jar.
- Clean build artifacts: `./gradlew clean`
- There is no test suite and no lint task configured in this repo.
- Kotlin toolchain is JVM 25 (`kotlin { jvmToolchain(25) }` in `build.gradle.kts`). Gradle wrapper is used for all builds (`./gradlew`, not a system-installed Gradle).
- `version` (used for the plugin jar name and injected into `plugin.yml`) comes from `gradle.properties`.

## Architecture

The plugin reads Minecraft's own per-player statistics on demand instead of maintaining any persistence layer of its own:

1. **`StatsCommand`** (`command/StatsCommand.kt`) handles `/stats`: it hops onto the invoking player's own entity thread/region via `PluginScheduler.runEntity` (required for Folia compatibility, see below) and, from there, calls **`VanillaStatsReader.read(player)`** (`stats/VanillaStatsReader.kt`) synchronously — no additional async hop is needed since there is no I/O to offload.
2. **`VanillaStatsReader`** (`stats/VanillaStatsReader.kt`) is a stateless `object` that walks `Material.values()` once: for every non-legacy block material it sums `Statistic.MINE_BLOCK` into the mined count, and for every non-legacy block material that is also a valid item it sums `Statistic.USE_ITEM` into the placed count. `Player.getStatistic(Statistic, Material)` can throw `IllegalArgumentException` for invalid statistic/material combinations (there is no public API to query valid combinations up front); this is caught narrowly per-material (never a broad `Exception`) and treated as 0.
3. **`PlayerVanillaStats`** (`stats/PlayerVanillaStats.kt`) is the plain data holder (`blocksMined`, `blocksPlaced`) returned by the reader.

### Scheduler abstraction (Folia + Paper/Spigot compatibility)

Because Folia uses per-region threading instead of Bukkit's single main thread, all scheduling goes through the **`PluginScheduler`** interface (`scheduler/PluginScheduler.kt`) rather than calling `Bukkit.getScheduler()` or entity schedulers directly:

- **`BukkitPluginScheduler`** — implementation for traditional Paper/Spigot, using `Bukkit.getScheduler()`.
- **`PaperFoliaPluginScheduler`** — implementation for Folia. Since Folia-only scheduler APIs (`Server#getAsyncScheduler`, `Entity#getScheduler`) don't exist in the Paper API this project compiles against, this implementation calls them via **reflection**.
- **`SchedulerFactory`** picks the right implementation at runtime by probing for the presence of `Server#getAsyncScheduler` — this method exists on both Folia and modern Paper (non-Folia), so the check is a capability probe rather than a Folia-vs-Paper identity check; only legacy Spigot/CraftBukkit servers fall back to `BukkitPluginScheduler`.

Now that there is no database I/O to offload, the scheduler's role is purely to guarantee entity-thread safety (Folia requires reads/replies for a given player to happen on that player's owning thread/region) rather than to move blocking work off the main thread. Any new code that needs to schedule work (async task, or getting back onto an entity's thread) must still go through `PluginScheduler`, never call Bukkit/Folia scheduler APIs directly — otherwise it will break on one of the two platforms.

### Plugin entry point

`PlayerStats.kt` (the `JavaPlugin` subclass, at the package root) wires the scheduler and `StatsCommand` together in `onEnable()`. `onDisable()` only logs, since there is no buffered state to flush.

`plugin.yml` declares the `stats` command and `api-version: '26.1'`; the `main` class and `version` (templated from `gradle.properties`) are injected via Gradle's `processResources` filtering (see `build.gradle.kts`).

## Known limitations / caveats

- **Historical data reset**: Prior to this migration, stats were tracked in a plugin-owned SQLite database. After migrating to Minecraft's built-in `Player.getStatistic` API, that historical data (especially placed-block counts) is not carried over — `/stats` will appear to reset to whatever vanilla has already tracked for that player (which may be 0 for placements if the player predates this migration on this world). This was a deliberate, accepted trade-off.
- **Seed-like blocks are undercounted for "placed"**: For items whose placed Material differs from the item Material (e.g. Wheat Seeds → Wheat crop, Carrot item → Carrots crop), `Statistic.USE_ITEM` is keyed on the *item* Material, which for these cases is not itself `isBlock`, so `VanillaStatsReader` (which only sums `USE_ITEM` over materials that are both `isBlock` and `isItem`) does not count these placements. This is a known, accepted limitation of the vanilla-statistics-based approach.
- **Possible overcounting for "use existing block" interactions (unverified)**: Vanilla only awards `Stats.ITEM_USED` for a block item on a successful *placement* (via `BlockItem.useOn`), not on other interactions — so using a held block item on an existing block of a different kind (e.g. feeding a composter, charging a respawn anchor with glowstone) is not expected to increment `USE_ITEM` for that item. This has not been empirically confirmed against a live server; if `/stats` placed-counts look inflated relative to actual placements, this is the first place to check.

## Manual verification (no automated test suite exists)

Since there is no test suite, verify changes to the stats-reading logic by hand against a local Paper and/or Folia test server:

1. Run `/stats` as a fresh player → expect `採掘数: 0` / `設置数: 0`.
2. Mine and place a handful of different block types → run `/stats` again and cross-check the counts against the vanilla in-game statistics screen (pause menu → Statistics), which reads the same underlying per-player data and serves as an independent oracle.
3. Restart the server (stop/start, not `/reload`) → confirm the counts are unchanged and that no plugin-owned data file (e.g. `stats.db`) is created under `plugins/player-stats/`.
4. If testing on Folia, repeat the mine/place/`/stats` sequence to confirm the single `PluginScheduler.runEntity` hop in `StatsCommand` completes without thread-ownership exceptions.
