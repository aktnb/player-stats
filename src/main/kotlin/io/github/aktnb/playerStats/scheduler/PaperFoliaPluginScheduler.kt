package io.github.aktnb.playerStats.scheduler

import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.TimeUnit

class PaperFoliaPluginScheduler(
    private val plugin: JavaPlugin,
): PluginScheduler {

    override fun runAsync(task: () -> Unit) {
        plugin.server.asyncScheduler.runNow(plugin) { task() }
    }

    override fun runAsyncTimer(initialDelayTicks: Long, periodTicks: Long, task: () -> Unit) {
        val initialDelayMillis = initialDelayTicks * 50L
        val periodMillis = periodTicks * 50L

        plugin.server.asyncScheduler.runAtFixedRate(
            plugin,
            { task() },
            initialDelayMillis,
            periodMillis,
            TimeUnit.MILLISECONDS
        )
    }

    override fun runEntity(
        entity: Entity,
        task: () -> Unit
    ) {
        entity.scheduler.run(plugin, { task() }, null)
    }
}
