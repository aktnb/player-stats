package io.github.aktnb.playerStats.listener

import io.github.aktnb.playerStats.stats.StatsBuffer
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent

class StatsListener(
    private val buffer : StatsBuffer,
): Listener {

    @EventHandler(ignoreCancelled = true)
    fun onBlockBreak(event: BlockBreakEvent) {
        buffer.addMined(event.player.uniqueId, event.player.name)
    }

    @EventHandler(ignoreCancelled = true)
    fun onBlockPlace(event: BlockPlaceEvent) {
        buffer.addMined(event.player.uniqueId, event.player.name)
    }
}