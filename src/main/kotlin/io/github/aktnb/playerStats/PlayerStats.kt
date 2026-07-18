package io.github.aktnb.playerStats

import io.github.aktnb.playerStats.command.StatsCommand
import io.github.aktnb.playerStats.listener.StatsGuiListener
import org.bukkit.plugin.java.JavaPlugin

class PlayerStats : JavaPlugin() {

    override fun onEnable() {
        logger.info("Enabling PlayerStats")
        try {
            server.pluginManager.registerEvents(
                StatsGuiListener(plugin = this),
                this
            )

            val statsCommand = getCommand("stats")
            if (statsCommand == null) {
                logger.warning("Command 'stats' is not defined in plugin.yml")
            } else {
                val executor = StatsCommand(plugin = this)
                statsCommand.setExecutor(executor)
                statsCommand.tabCompleter = executor
            }

            logger.info("Enabled PlayerStats")
        } catch (e: Exception) {
            logger.severe("Failed to enable PlayerStats")
            e.printStackTrace()
            server.pluginManager.disablePlugin(this)
        }
    }

    override fun onDisable() {
        logger.info("Disabling PlayerStats")
        logger.info("Disabled PlayerStats")
    }
}
