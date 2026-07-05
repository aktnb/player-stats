package io.github.aktnb.playerStats

import io.github.aktnb.playerStats.command.StatsCommand
import io.github.aktnb.playerStats.listener.StatsListener
import io.github.aktnb.playerStats.repository.SQLiteProvider
import io.github.aktnb.playerStats.repository.StatsRepository
import io.github.aktnb.playerStats.scheduler.PluginScheduler
import io.github.aktnb.playerStats.scheduler.SchedulerFactory
import io.github.aktnb.playerStats.stats.StatsBuffer
import io.github.aktnb.playerStats.stats.StatsFlushService
import org.bukkit.plugin.java.JavaPlugin

class PlayerStats : JavaPlugin() {

    private lateinit var sqliteProvider: SQLiteProvider
    private lateinit var repository: StatsRepository
    private lateinit var buffer: StatsBuffer
    private lateinit var flushService: StatsFlushService
    private lateinit var scheduler: PluginScheduler

    override fun onEnable() {
        logger.info("Enabling PlayerStats")
        try {
            if (!dataFolder.exists()) dataFolder.mkdir()

            scheduler = SchedulerFactory.create(this)

            sqliteProvider = SQLiteProvider(this)
            sqliteProvider.init()

            repository = StatsRepository(sqliteProvider)
            repository.init()

            buffer = StatsBuffer()
            flushService = StatsFlushService(
                buffer = buffer,
                repository = repository,
            )

            server.pluginManager.registerEvents(
                StatsListener(buffer),
                this
            )

            val statsCommand = getCommand("stats")
            if (statsCommand == null) {
                logger.warning("Command 'stats' is not defined in plugin.yml")
            } else {
                statsCommand.setExecutor(
                    StatsCommand(
                        repository = repository,
                        scheduler = scheduler
                    )
                )
            }

            scheduler.runAsyncTimer(
                initialDelayTicks = 20L * 5,
                periodTicks = 20L * 5,
            ) {
                flushService.flush()
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

        try {
            if (::flushService.isInitialized) {
                flushService.flush()
            }

            logger.info("Disabled PlayerStats")
        } catch (e: Exception) {
            logger.severe("Failed to disable PlayerStats")
            e.printStackTrace()
        }
    }
}
