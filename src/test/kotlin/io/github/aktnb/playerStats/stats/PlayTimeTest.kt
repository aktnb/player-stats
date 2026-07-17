package io.github.aktnb.playerStats.stats

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PlayTimeTest {

    // 20 tick = 1秒, 1200 tick = 1分, 72000 tick = 1時間, 1728000 tick = 1日
    private val ticksPerMinute = 1200L
    private val ticksPerHour = 72000L
    private val ticksPerDay = 1728000L

    @Test
    fun `zero ticks is all zero`() {
        assertEquals(PlayTime(0L, 0L, 0L), PlayTime.fromTicks(0L))
    }

    @Test
    fun `negative ticks are treated as zero`() {
        assertEquals(PlayTime(0L, 0L, 0L), PlayTime.fromTicks(-1L))
    }

    @Test
    fun `ticks below one minute floor to zero minutes`() {
        assertEquals(PlayTime(0L, 0L, 0L), PlayTime.fromTicks(ticksPerMinute - 1))
    }

    @Test
    fun `exactly one minute`() {
        assertEquals(PlayTime(0L, 0L, 1L), PlayTime.fromTicks(ticksPerMinute))
    }

    @Test
    fun `sub-minute remainder is truncated`() {
        // 45分 + 19tick(1分未満) → 45分
        assertEquals(PlayTime(0L, 0L, 45L), PlayTime.fromTicks(45 * ticksPerMinute + 19))
    }

    @Test
    fun `exactly one hour`() {
        assertEquals(PlayTime(0L, 1L, 0L), PlayTime.fromTicks(ticksPerHour))
    }

    @Test
    fun `three hours twenty minutes`() {
        assertEquals(PlayTime(0L, 3L, 20L), PlayTime.fromTicks(3 * ticksPerHour + 20 * ticksPerMinute))
    }

    @Test
    fun `exactly one day`() {
        assertEquals(PlayTime(1L, 0L, 0L), PlayTime.fromTicks(ticksPerDay))
    }

    @Test
    fun `two days five hours zero minutes`() {
        assertEquals(PlayTime(2L, 5L, 0L), PlayTime.fromTicks(2 * ticksPerDay + 5 * ticksPerHour))
    }

    @Test
    fun `two days three hours forty-five minutes (all units non-zero)`() {
        assertEquals(
            PlayTime(2L, 3L, 45L),
            PlayTime.fromTicks(2 * ticksPerDay + 3 * ticksPerHour + 45 * ticksPerMinute),
        )
    }

    @Test
    fun `displayUnits omits leading zero units`() {
        assertEquals(
            listOf(PlayTimeUnitValue(PlayTimeUnit.MINUTE, 45L)),
            PlayTime(0L, 0L, 45L).displayUnits(),
        )
    }

    @Test
    fun `displayUnits keeps hour and minute`() {
        assertEquals(
            listOf(
                PlayTimeUnitValue(PlayTimeUnit.HOUR, 3L),
                PlayTimeUnitValue(PlayTimeUnit.MINUTE, 20L),
            ),
            PlayTime(0L, 3L, 20L).displayUnits(),
        )
    }

    @Test
    fun `displayUnits keeps days only`() {
        assertEquals(
            listOf(PlayTimeUnitValue(PlayTimeUnit.DAY, 2L)),
            PlayTime(2L, 0L, 0L).displayUnits(),
        )
    }

    @Test
    fun `displayUnits omits trailing zero minutes`() {
        assertEquals(
            listOf(
                PlayTimeUnitValue(PlayTimeUnit.DAY, 2L),
                PlayTimeUnitValue(PlayTimeUnit.HOUR, 5L),
            ),
            PlayTime(2L, 5L, 0L).displayUnits(),
        )
    }

    @Test
    fun `displayUnits omits middle zero hour`() {
        assertEquals(
            listOf(
                PlayTimeUnitValue(PlayTimeUnit.DAY, 2L),
                PlayTimeUnitValue(PlayTimeUnit.MINUTE, 30L),
            ),
            PlayTime(2L, 0L, 30L).displayUnits(),
        )
    }

    @Test
    fun `displayUnits shows single zero minute when all zero`() {
        assertEquals(
            listOf(PlayTimeUnitValue(PlayTimeUnit.MINUTE, 0L)),
            PlayTime(0L, 0L, 0L).displayUnits(),
        )
    }
}
