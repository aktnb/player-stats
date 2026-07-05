package io.github.aktnb.playerStats.scheduler

import org.bukkit.plugin.java.JavaPlugin

object SchedulerFactory {

    fun create(plugin: JavaPlugin): PluginScheduler {
        return if (hasAsyncScheduler(plugin)) {
            PaperFoliaPluginScheduler(plugin)
        } else {
            BukkitPluginScheduler(plugin)
        }
    }

    private fun hasAsyncScheduler(plugin: JavaPlugin): Boolean {
        return try {
            plugin.server.javaClass.getMethod("getAsyncScheduler")
            true
        } catch (_: NoSuchMethodException) {
            false
        }
    }
}
