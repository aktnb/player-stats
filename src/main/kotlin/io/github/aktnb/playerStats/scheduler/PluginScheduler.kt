package io.github.aktnb.playerStats.scheduler

import org.bukkit.entity.Entity

interface PluginScheduler {
    fun runAsync(task: () -> Unit)
    fun runAsyncTimer(initialDelayTicks: Long, periodTicks: Long, task: () -> Unit)
    fun runEntity(entity: Entity, task: () -> Unit)
}
