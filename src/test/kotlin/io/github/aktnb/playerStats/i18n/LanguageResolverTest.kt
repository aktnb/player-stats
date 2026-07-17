package io.github.aktnb.playerStats.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Locale

class LanguageResolverTest {

    @Test
    fun `ja resolves to JA`() {
        assertEquals(Language.JA, LanguageResolver.resolve(Locale.of("ja")))
    }

    @Test
    fun `ja_JP resolves to JA`() {
        assertEquals(Language.JA, LanguageResolver.resolve(Locale.of("ja", "JP")))
    }

    @Test
    fun `en resolves to EN`() {
        assertEquals(Language.EN, LanguageResolver.resolve(Locale.of("en")))
    }

    @Test
    fun `en_US resolves to EN`() {
        assertEquals(Language.EN, LanguageResolver.resolve(Locale.of("en", "US")))
    }

    @Test
    fun `unknown locale zh falls back to EN`() {
        assertEquals(Language.EN, LanguageResolver.resolve(Locale.of("zh")))
    }

    @Test
    fun `unknown locale zh_CN falls back to EN`() {
        assertEquals(Language.EN, LanguageResolver.resolve(Locale.of("zh", "CN")))
    }

    @Test
    fun `unknown locale fr falls back to EN`() {
        assertEquals(Language.EN, LanguageResolver.resolve(Locale.of("fr")))
    }

    @Test
    fun `Locale ROOT (empty language code) falls back to EN`() {
        assertEquals(Language.EN, LanguageResolver.resolve(Locale.ROOT))
    }

    @Test
    fun `uppercase language code JA is normalized to ja and resolves to JA`() {
        // java.util.Locale は言語コードを内部的に小文字に正規化するため、"JA" は "ja" と同じ扱いになる。
        assertEquals(Language.JA, LanguageResolver.resolve(Locale.of("JA")))
    }
}
