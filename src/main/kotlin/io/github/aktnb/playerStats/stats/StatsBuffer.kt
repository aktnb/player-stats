package io.github.aktnb.playerStats.stats

import java.util.UUID

class StatsBuffer {

    private val lock = Any()
    private val deltas = mutableMapOf<UUID, PlayerStatsDelta>()

    fun addMined(uuid: UUID, name: String) {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val stats = deltas.getOrPut(uuid) {
                PlayerStatsDelta(uuid, name)
            }
            stats.name = name
            stats.blockMined++
            stats.lastSeenAt = now
        }
    }

    fun addPlaced(uuid: UUID, name: String) {
        synchronized(lock) {
            val now = System.currentTimeMillis()
            val stats = deltas.getOrPut(uuid) {
                PlayerStatsDelta(uuid, name)
            }
            stats.name = name
            stats.blockPlaced++
            stats.lastSeenAt = now
        }
    }

    fun drain(): List<PlayerStatsDelta> {
        synchronized(lock) {
            val snapshot = deltas.values.map { it.copy() }
            deltas.clear()
            return snapshot
        }
    }

    fun mergeBlock(failedDeltas: List<PlayerStatsDelta>) {
        synchronized(lock) {
            for (delta in failedDeltas) {
                val stats = deltas.getOrPut(delta.uuid) {
                    PlayerStatsDelta(delta.uuid, delta.name)
                }
                stats.name = delta.name
                stats.blockMined += delta.blockMined
                stats.blockPlaced += delta.blockPlaced
                stats.lastSeenAt = maxOf(stats.lastSeenAt, delta.lastSeenAt)
            }
        }
    }
}