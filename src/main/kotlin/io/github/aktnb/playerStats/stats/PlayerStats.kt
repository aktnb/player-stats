package io.github.aktnb.playerStats.stats

import java.util.UUID

data class PlayerStats(
    val uuid: UUID,
    val name: String,
    val blocksMined: Long,
    val blocksPlaced: Long,
    val lastSeenAt: Long,
)
