package io.github.aktnb.playerStats.stats

data class PlayerVanillaStats(
    val blocksMined: Long,
    val blocksPlaced: Long,
    val mobKills: Long,
    /**
     * 累計プレイ時間の生値。`Statistic.PLAY_ONE_MINUTE` は名称に反して実際の値はtick単位
     * (20 tick = 1秒)。日時分への分解は表示直前に [PlayTime.fromTicks] で行うため、ここでは
     * 生tickのまま保持する。
     */
    val playTimeTicks: Long,
)
