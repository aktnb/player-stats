package io.github.aktnb.playerStats.stats

import org.bukkit.Material
import org.bukkit.OfflinePlayer
import org.bukkit.Statistic

/**
 * Reads a player's block-mining/placing counts directly from Minecraft's own
 * per-player statistics (`Statistic.MINE_BLOCK` / `Statistic.USE_ITEM`),
 * instead of a plugin-owned persistence layer.
 */
object VanillaStatsReader {

    private val blockMaterials: List<Material> = Material.values()
        .filter { !it.isLegacy && it.isBlock }

    private val itemizableBlockMaterials: List<Material> = blockMaterials.filter { it.isItem }

    fun read(player: OfflinePlayer): PlayerVanillaStats {
        var blocksMined = 0L
        for (material in blockMaterials) {
            blocksMined += getStatisticSafely(player, Statistic.MINE_BLOCK, material)
        }

        var blocksPlaced = 0L
        for (material in itemizableBlockMaterials) {
            blocksPlaced += getStatisticSafely(player, Statistic.USE_ITEM, material)
        }

        return PlayerVanillaStats(
            blocksMined = blocksMined,
            blocksPlaced = blocksPlaced,
        )
    }

    /**
     * ブロック別の採掘数内訳を返す。`itemizableBlockMaterials`(isItemなブロックのみ)を対象とするため、
     * 水・溶岩など非アイテム化ブロックは内訳に含まれない。したがって本関数が返すリストの合計値は
     * [read] が返す `blocksMined`(全ブロック対象)と厳密には一致しない場合がある。
     */
    fun readMiningBreakdown(player: OfflinePlayer): List<MaterialMiningCount> {
        return itemizableBlockMaterials
            .map { MaterialMiningCount(it, getStatisticSafely(player, Statistic.MINE_BLOCK, it).toLong()) }
            .filter { it.count > 0 }
            .sortedWith(compareByDescending<MaterialMiningCount> { it.count }.thenBy { it.material.name })
    }

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
}
