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

    private val placeableBlockMaterials: List<Material> = blockMaterials.filter { it.isItem }

    fun read(player: OfflinePlayer): PlayerVanillaStats {
        var blocksMined = 0L
        for (material in blockMaterials) {
            blocksMined += getStatisticSafely(player, Statistic.MINE_BLOCK, material)
        }

        var blocksPlaced = 0L
        for (material in placeableBlockMaterials) {
            blocksPlaced += getStatisticSafely(player, Statistic.USE_ITEM, material)
        }

        return PlayerVanillaStats(
            blocksMined = blocksMined,
            blocksPlaced = blocksPlaced,
        )
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
