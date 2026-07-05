package io.github.aktnb.playerStats.scheduler

import org.bukkit.entity.Entity
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class PaperFoliaPluginScheduler(
    private val plugin: JavaPlugin,
): PluginScheduler {

    override fun runAsync(task: () -> Unit) {
        val asyncScheduler = getAsyncScheduler()

        val runNowMethod = asyncScheduler.javaClass.methods.first {
            it.name == "runNow" && it.parameterTypes.size == 2
        }

        runNowMethod.invoke(asyncScheduler, plugin, Consumer<Any> { task() })
    }

    override fun runAsyncTimer(initialDelayTicks: Long, periodTicks: Long, task: () -> Unit) {
        val asyncScheduler = getAsyncScheduler()

        val runAtFixedRateMethod = asyncScheduler.javaClass.methods.first {
            it.name == "runAtFixedRate" &&
                    it.parameterTypes.size == 5
        }

        val initialDelayMillis = initialDelayTicks * 50L
        val periodMillis = periodTicks * 50L

        val consumer = Consumer<Any> {
            task()
        }

        runAtFixedRateMethod.invoke(
            asyncScheduler,
            plugin,
            consumer,
            initialDelayMillis,
            periodMillis,
            TimeUnit.MILLISECONDS
        )
    }

    override fun runEntity(
        entity: Entity,
        task: () -> Unit
    ) {
        val schedulerMethod = entity.javaClass.methods.first {
            it.name == "getScheduler" && it.parameterTypes.isEmpty()
        }

        val entityScheduler = schedulerMethod.invoke(entity)

        val runMethod = entityScheduler.javaClass.methods.first {
            it.name == "run" && it.parameterTypes.size == 3
        }

        runMethod.invoke(
            entityScheduler,
            plugin,
            Consumer<Any> {
                task()
            },
            null
        )
    }

    private fun getAsyncScheduler(): Any {
        val method = plugin.server.javaClass.getMethod("getAsyncScheduler")
        return method.invoke(plugin.server)
    }
}