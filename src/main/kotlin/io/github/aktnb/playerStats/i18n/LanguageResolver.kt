package io.github.aktnb.playerStats.i18n

import java.util.Locale

/**
 * クライアントロケールから表示言語([Language])を決定する、Bukkit非依存の純粋関数。
 *
 * [Locale.getLanguage] が `"ja"` の場合のみ日本語とし、それ以外(未知のロケールを含む)はすべて
 * 英語にフォールバックする。単純な二値判定にすることで、将来ロケールの生文字列がそのまま
 * Mapキーやファイルパスの組み立てに使われるような実装ミスを避けやすくしている。
 */
object LanguageResolver {
    fun resolve(locale: Locale): Language =
        if (locale.language == "ja") Language.JA else Language.EN
}
