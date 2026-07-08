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
                statsCommand.setExecutor(
                    StatsCommand(plugin = this)
                )
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
