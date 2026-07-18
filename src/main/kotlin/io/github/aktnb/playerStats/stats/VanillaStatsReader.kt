package io.github.aktnb.playerStats.stats

import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.Statistic
import org.bukkit.entity.EntityType
import org.bukkit.entity.Mob

/**
 * Reads a player's block-mining/placing counts and mob-kill counts directly from
 * Minecraft's own per-player statistics (`Statistic.MINE_BLOCK` / `Statistic.USE_ITEM`
 * / `Statistic.KILL_ENTITY`), instead of a plugin-owned persistence layer.
 */
object VanillaStatsReader {

    private val blockMaterials: List<Material> = Material.values()
        .filter { !it.isLegacy && it.isBlock }

    private val itemizableBlockMaterials: List<Material> = blockMaterials.filter { it.isItem }

    /**
     * キル数集計の対象とするエンティティ種別。`org.bukkit.entity.Mob` を実装する [EntityType] のみに
     * 明示的に絞ることで、PLAYER(PvP)を `Statistic.KILL_ENTITY` のAPI挙動に依存せず確実に除外している
     * (PLAYER の entityClass は `Mob` を実装しないため対象外になる)。ARMOR_STAND など非Mobエンティティも同様に除外される。
     * `entityClass` は一部の内部種別で null になりうるためnull安全に判定する。
     */
    private val mobEntityTypes: List<EntityType> = EntityType.entries
        .filter { et -> et.entityClass?.let { Mob::class.java.isAssignableFrom(it) } == true }

    fun read(player: OfflinePlayer): PlayerVanillaStats {
        var blocksMined = 0L
        for (material in blockMaterials) {
            blocksMined += getStatisticSafely(player, Statistic.MINE_BLOCK, material)
        }

        var blocksPlaced = 0L
        for (material in itemizableBlockMaterials) {
            blocksPlaced += getStatisticSafely(player, Statistic.USE_ITEM, material)
        }

        var mobKills = 0L
        for (entityType in mobEntityTypes) {
            mobKills += getStatisticSafely(player, Statistic.KILL_ENTITY, entityType)
        }

        // `Statistic.PLAY_ONE_MINUTE` は untyped 統計(Material/EntityTypeを取らない)で、
        // その名に反して実際の値はtick単位。日時分への分解は表示側の [PlayTime.fromTicks] に委ねる。
        val playTimeTicks = player.getStatistic(Statistic.PLAY_ONE_MINUTE).toLong()

        return PlayerVanillaStats(
            blocksMined = blocksMined,
            blocksPlaced = blocksPlaced,
            mobKills = mobKills,
            playTimeTicks = playTimeTicks,
        )
    }

    private fun readBreakdown(player: OfflinePlayer, statistic: Statistic, materials: List<Material>): List<MaterialStatCount> {
        return materials
            .map { MaterialStatCount(it, getStatisticSafely(player, statistic, it).toLong()) }
            .filter { it.count > 0 }
            .sortedWith(compareByDescending<MaterialStatCount> { it.count }.thenBy { it.material.name })
    }

    /**
     * ブロック別の採掘数内訳を返す。`itemizableBlockMaterials`(isItemなブロックのみ)を対象とするため、
     * 水・溶岩など非アイテム化ブロックは内訳に含まれない。したがって本関数が返すリストの合計値は
     * [read] が返す `blocksMined`(全ブロック対象)と厳密には一致しない場合がある。
     */
    fun readMiningBreakdown(player: OfflinePlayer): List<MaterialStatCount> =
        readBreakdown(player, Statistic.MINE_BLOCK, itemizableBlockMaterials)

    /**
     * ブロック別の設置数内訳を返す。[read] の `blocksPlaced` と同じ `itemizableBlockMaterials` に対して
     * `Statistic.USE_ITEM` を集計するため、こちらは合計値がサマリーの `blocksPlaced` と厳密に一致する。
     * ただしキーはアイテム側のMaterialであり、種→作物のように設置後のブロックがアイテムと異なる場合、
     * その設置は内訳に反映されない(既存の「seed系ブロック未カウント」と同種のトレードオフ)。
     */
    fun readPlacementBreakdown(player: OfflinePlayer): List<MaterialStatCount> =
        readBreakdown(player, Statistic.USE_ITEM, itemizableBlockMaterials)

    /**
     * エンティティ種別ごとのキル数内訳を返す。[read] の `mobKills` と同じ [mobEntityTypes] を対象に
     * `Statistic.KILL_ENTITY` を集計するため、内訳の合計値はサマリーの `mobKills` と厳密に一致する。
     * count>0 のエントリのみを count降順→種別名昇順で返す(既存の [readBreakdown] と同じ思想)。
     */
    fun readMobKillBreakdown(player: OfflinePlayer): List<EntityStatCount> =
        mobEntityTypes
            .map { EntityStatCount(it, getStatisticSafely(player, Statistic.KILL_ENTITY, it).toLong()) }
            .filter { it.count > 0 }
            .sortedWith(compareByDescending<EntityStatCount> { it.count }.thenBy { it.entityType.name })

    /**
     * `Player.getStatistic(Statistic, Material)` throws [IllegalArgumentException]
     * for invalid statistic/material combinations, and there is no public API to
     * query which combinations are valid up front. We catch narrowly on that type
     * only (never a broad `Exception`) and treat an invalid combination as 0.
     */
    private fun getStatisticSafely(player: OfflinePlayer, statistic: Statistic, material: Material): Int {
        return try {
            player.getStatistic(statistic, material)
        } catch (e: IllegalArgumentException) {
            0
        }
    }

    /**
     * [getStatisticSafely] の [EntityType] 版。`Player.getStatistic(Statistic, EntityType)` も無効な
     * 統計/エンティティの組み合わせで [IllegalArgumentException] を投げるため、Material版と同様に
     * その型に限定して narrow catch し、無効な組み合わせは0として扱う。Material版とは引数の型で分離している。
     */
    private fun getStatisticSafely(player: OfflinePlayer, statistic: Statistic, entityType: EntityType): Int {
        return try {
            player.getStatistic(statistic, entityType)
        } catch (e: IllegalArgumentException) {
            0
        }
    }
}
