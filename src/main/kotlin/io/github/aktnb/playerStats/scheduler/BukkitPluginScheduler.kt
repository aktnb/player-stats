package io.github.aktnb.playerStats.scheduler

import org.bukkit.Bukkit
import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin

class BukkitPluginScheduler(
    private val plugin: JavaPlugin
) : PluginScheduler {
    override fun runAsync(task: () -> Unit) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, task)
    }

    override fun runAsyncTimer(initialDelayTicks: Long, periodTicks: Long, task: () -> Unit) {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, initialDelayTicks, periodTicks)
    }

    override fun runEntity(entity: Entity, task: () -> Unit) {
        Bukkit.getScheduler().runTask(plugin, task)
    }
}
