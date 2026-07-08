# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

`player-stats` is a Paper/Spigot-compatible (Bukkit-based) Minecraft server plugin written in Kotlin. It tracks per-player block-mining/placing counts and mob-kill counts and exposes them via an inventory GUI, opened either with the `/stats` command (your own stats) or by right-clicking another online player (their stats). Both read Minecraft's own standard statistics (`Player.getStatistic`) on demand — the plugin itself holds no persistence layer.

## Build & Development Commands

- Build the plugin jar (shadow/fat jar with dependencies bundled): `./gradlew build`
- Build only the shadow jar: `./gradlew shadowJar`
- Output jars land in `build/libs/` — `player-stats-<version>-all.jar` is the fat jar to deploy to a server's `plugins/` folder; `player-stats-<version>.jar` is the thin jar.
- Clean build artifacts: `./gradlew clean`
- There is no test suite and no lint task configured in this repo.
- Kotlin toolchain is JVM 25 (`kotlin { jvmToolchain(25) }` in `build.gradle.kts`). Gradle wrapper is used for all builds (`./gradlew`, not a system-installed Gradle).
- `version` (used for the plugin jar name and injected into `plugin.yml`) comes from `gradle.properties`.

## Architecture

The plugin reads Minecraft's own per-player statistics on demand instead of maintaining any persistence layer of its own, and displays them through a shared inventory GUI:

1. **`StatsCommand`** (`command/StatsCommand.kt`) handles `/stats`: it schedules its work via `Bukkit.getScheduler().runTask` (deferring to the next tick so it never runs inline with command dispatch) and, from there, calls **`VanillaStatsReader.read(player)`** (`stats/VanillaStatsReader.kt`) synchronously — no additional async hop is needed since there is no I/O to offload — then opens the GUI built by `StatsGuiFactory` for the player themself.
2. **`StatsGuiListener`** (`listener/StatsGuiListener.kt`) listens for `PlayerInteractEntityEvent`: right-clicking another online player reads *that player's* stats and opens the same GUI for the interactor. The read and the GUI open both happen inside a single `Bukkit.getScheduler().runTask` callback (scheduled for the next tick so it doesn't interfere with the click/interact event still being processed). A short per-interactor cooldown avoids spamming re-opens/re-reads on repeated right-clicks. The listener also cancels all clicks/drags inside a stats GUI (identified via `StatsGuiHolder`) to keep it view-only.
3. **`VanillaStatsReader`** (`stats/VanillaStatsReader.kt`) is a stateless `object` that pre-computes (at class-init time) the list of non-legacy block materials once: it sums `Statistic.MINE_BLOCK` over all of them for the mined count, and `Statistic.USE_ITEM` over the subset that are also a valid item for the placed count. `Player.getStatistic(Statistic, Material)` can throw `IllegalArgumentException` for invalid statistic/material combinations (there is no public API to query valid combinations up front); this is caught narrowly per-material (never a broad `Exception`) and treated as 0.
4. **`PlayerVanillaStats`** (`stats/PlayerVanillaStats.kt`) is the plain data holder (`blocksMined`, `blocksPlaced`) returned by the reader.
5. **`StatsGuiFactory`** (`gui/StatsGuiFactory.kt`) builds the fixed 9-slot, view-only inventory (a pickaxe for mined count, a grass block for placed count, glass-pane filler elsewhere), tagged with **`StatsGuiHolder`** (`gui/StatsGuiHolder.kt`) so listeners can recognize it as a stats GUI regardless of who it's being shown to.

### Plugin entry point

`PlayerStats.kt` (the `JavaPlugin` subclass, at the package root) wires itself (as `Plugin`), `StatsCommand`, and `StatsGuiListener` together in `onEnable()`. `onDisable()` only logs, since there is no buffered state to flush.

`plugin.yml` declares the `stats` command and `api-version: '26.1'`; the `main` class and `version` (templated from `gradle.properties`) are injected via Gradle's `processResources` filtering (see `build.gradle.kts`).

## Known limitations / caveats

- **Historical data reset**: Prior to this migration, stats were tracked in a plugin-owned SQLite database. After migrating to Minecraft's built-in `Player.getStatistic` API, that historical data (especially placed-block counts) is not carried over — `/stats` will appear to reset to whatever vanilla has already tracked for that player (which may be 0 for placements if the player predates this migration on this world). This was a deliberate, accepted trade-off.
- **Seed-like blocks are undercounted for "placed"**: For items whose placed Material differs from the item Material (e.g. Wheat Seeds → Wheat crop, Carrot item → Carrots crop), `Statistic.USE_ITEM` is keyed on the *item* Material, which for these cases is not itself `isBlock`, so `VanillaStatsReader` (which only sums `USE_ITEM` over materials that are both `isBlock` and `isItem`) does not count these placements. This is a known, accepted limitation of the vanilla-statistics-based approach.
- **Possible overcounting for "use existing block" interactions (unverified)**: Vanilla only awards `Stats.ITEM_USED` for a block item on a successful *placement* (via `BlockItem.useOn`), not on other interactions — so using a held block item on an existing block of a different kind (e.g. feeding a composter, charging a respawn anchor with glowstone) is not expected to increment `USE_ITEM` for that item. This has not been empirically confirmed against a live server; if `/stats` placed-counts look inflated relative to actual placements, this is the first place to check.
- **採掘内訳とサマリー合計の不一致**: ピッケルクリックで開くブロック別採掘内訳は、アイコン化できるブロック(`isItem`なMaterial)のみを対象とするため、水・溶岩など非アイテム化ブロックの採掘数を含まない。したがって内訳の合計値はサマリー画面の採掘数合計(`blocksMined`、全ブロック対象)と厳密には一致しない場合がある。既存の「seed系ブロック未カウント」と同種の許容トレードオフ。
- **設置内訳はアイテム基準**: 草ブロッククリックで開く設置内訳は`Statistic.USE_ITEM`を`itemizableBlockMaterials`(アイテム側Material)に対して集計するため、種→作物のように設置後のブロックが使用アイテムと異なる場合、その設置は内訳に反映されない(既存の「seed系ブロック未カウント」と同種の許容トレードオフ)。なお、こちらは採掘内訳と異なり、内訳の合計値はサマリー画面の設置数合計(`blocksPlaced`)と厳密に一致する(サマリーも同じ`itemizableBlockMaterials`を対象にしているため)。
- **キル数は Mob 実装エンティティのみ対象**: 鉄の剣クリックで開くキル内訳、およびサマリーのキル数合計(`mobKills`)は、`Statistic.KILL_ENTITY`を「`org.bukkit.entity.Mob`を実装する`EntityType`」に限定して集計する。これにより PLAYER(PvP キル)は API 挙動に依存せず確実に除外されるが、`Mob`を実装しないエンティティ(ARMOR_STAND や一部の非Mob生物など)のキルもカウントされない。`mobKills`とキル内訳は同じ`mobEntityTypes`を対象とするため、内訳の合計値はサマリーの`mobKills`と厳密に一致する。
- **キル内訳のアイコン解決**: キル内訳の各エンティティアイコンは`EntityIconResolver`が Mob Head(`mobHeadIcons`)→`<TYPE>_SPAWN_EGG`→個別フォールバック(GIANT→ZOMBIE_HEAD、ILLUSIONER→BOW)→既定`IRON_SWORD`の順で解決する確定的関数で、例外は投げない。Mob Head アイテムが実在する CREEPER・ENDER_DRAGON・PIGLIN・SKELETON・WITHER_SKELETON・ZOMBIE はスポーンエッグより Mob Head アイコンが優先される(PLAYER_HEADも実在するが、PLAYERは`mobEntityTypes`に含まれずキル内訳に出現しないため`mobHeadIcons`には含めていない)。スポーンエッグが存在しない新規Mobが将来追加された場合は既定の`IRON_SWORD`アイコンで表示される。

## Manual verification (no automated test suite exists)

Since there is no test suite, verify changes to the stats-reading logic by hand against a local Paper/Spigot test server:

1. Run `/stats` as a fresh player → expect the GUI to show `0` on both the pickaxe (mined) and grass block (placed) items.
2. Mine and place a handful of different block types → run `/stats` again and cross-check the counts against the vanilla in-game statistics screen (pause menu → Statistics), which reads the same underlying per-player data and serves as an independent oracle.
3. Right-click another online player → confirm their GUI (not your own) opens with their counts, that the GUI is fully view-only (clicks/drags do nothing), and that rapid repeated right-clicks are throttled by the cooldown rather than reopening/re-reading every time.
4. Kill several kinds of both hostile mobs (e.g. zombies, skeletons) and non-hostile mobs (e.g. villagers, iron golems), then open `/stats` and click the iron sword → confirm the kill-breakdown screen shows a per-entity-type breakdown whose total matches the summary's kill count (`mobKills`). Both are summed over the same `mobEntityTypes`, so they must agree exactly.
5. Kill another player (PvP) → confirm `mobKills` does **not** increase. The code filters `Statistic.KILL_ENTITY` by the `Mob` interface (`PLAYER` does not implement it), so PvP kills are expected to be excluded; still verify this against a live server, since this depends on vanilla's actual `Statistic.KILL_ENTITY` behavior for player kills.
6. Kill a `GIANT` and an `ILLUSIONER` (both lack a spawn egg; spawn them via `/summon` since they are otherwise hard to obtain) → confirm the kill-breakdown icons fall back correctly (`GIANT` → zombie head, `ILLUSIONER` → bow) as resolved by `EntityIconResolver`, rather than rendering a missing/default icon.
7. Restart the server (stop/start, not `/reload`) → confirm the counts are unchanged and that no plugin-owned data file (e.g. `stats.db`) is created under `plugins/player-stats/`.
8. Kill a `CREEPER`, `ENDER_DRAGON`(`/summon` recommended), `PIGLIN`, `SKELETON`, `WITHER_SKELETON`, and `ZOMBIE` → confirm each shows its dedicated Mob Head icon (e.g. `ZOMBIE_HEAD`, `SKELETON_SKULL`) in the kill breakdown instead of its spawn egg, as resolved by `EntityIconResolver`'s `mobHeadIcons` priority.
