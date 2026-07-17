package io.github.aktnb.playerStats.i18n

import io.github.aktnb.playerStats.stats.PlayTime
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * [Messages.formatPlayTime] は Adventure/Bukkit に依存しない純粋な文字列整形なのでJUnit可能。
 * JA/EN それぞれで単位ラベル・語順・ゼロ省略が期待どおりかを検証する。
 */
class MessagesFormatPlayTimeTest {

    private val ja = MessageCatalog.forLanguage(Language.JA)
    private val en = MessageCatalog.forLanguage(Language.EN)

    @Test
    fun `JA formats minutes only`() {
        assertEquals("45分", ja.formatPlayTime(PlayTime(0L, 0L, 45L)))
    }

    @Test
    fun `JA formats hours and minutes`() {
        assertEquals("3時間 20分", ja.formatPlayTime(PlayTime(0L, 3L, 20L)))
    }

    @Test
    fun `JA omits trailing zero minutes`() {
        assertEquals("2日 5時間", ja.formatPlayTime(PlayTime(2L, 5L, 0L)))
    }

    @Test
    fun `JA formats all three units`() {
        assertEquals("2日 3時間 45分", ja.formatPlayTime(PlayTime(2L, 3L, 45L)))
    }

    @Test
    fun `JA omits middle zero hour`() {
        assertEquals("2日 30分", ja.formatPlayTime(PlayTime(2L, 0L, 30L)))
    }

    @Test
    fun `JA shows zero minute when all zero`() {
        assertEquals("0分", ja.formatPlayTime(PlayTime(0L, 0L, 0L)))
    }

    @Test
    fun `EN formats minutes only`() {
        assertEquals("45m", en.formatPlayTime(PlayTime(0L, 0L, 45L)))
    }

    @Test
    fun `EN formats hours and minutes`() {
        assertEquals("3h 20m", en.formatPlayTime(PlayTime(0L, 3L, 20L)))
    }

    @Test
    fun `EN omits trailing zero minutes`() {
        assertEquals("2d 5h", en.formatPlayTime(PlayTime(2L, 5L, 0L)))
    }

    @Test
    fun `EN formats all three units`() {
        assertEquals("2d 3h 45m", en.formatPlayTime(PlayTime(2L, 3L, 45L)))
    }

    @Test
    fun `EN omits middle zero hour`() {
        assertEquals("2d 30m", en.formatPlayTime(PlayTime(2L, 0L, 30L)))
    }

    @Test
    fun `EN shows zero minute when all zero`() {
        assertEquals("0m", en.formatPlayTime(PlayTime(0L, 0L, 0L)))
    }
}
