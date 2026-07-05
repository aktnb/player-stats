package io.github.aktnb.playerStats.stats

import java.util.UUID

data class PlayerStatsDelta (
    val uuid: UUID,
    var name: String,
    var blockMined: Long = 0L,
    var blockPlaced: Long = 0L,
    var lastSeenAt: Long = System.currentTimeMillis(),
)
