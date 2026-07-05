package io.github.aktnb.playerStats.stats

import io.github.aktnb.playerStats.repository.StatsRepository
import java.util.concurrent.atomic.AtomicBoolean

class StatsFlushService(
    private val buffer: StatsBuffer,
    private val repository: StatsRepository
) {
    private val flushing = AtomicBoolean(false)

    fun flush() {
        if (!flushing.compareAndSet(false, true)) {
            return
        }

        val deltas = buffer.drain()

        try {
            if (deltas.isNotEmpty()) {
                repository.saveDeltas(deltas)
            }
        } catch (e: Exception) {
            buffer.mergeBlock(deltas)
            e.printStackTrace()
        } finally {
            flushing.set(false)
        }
    }
}