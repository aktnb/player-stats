package io.github.aktnb.playerStats.i18n

/**
 * プラグインが表示をサポートする言語。
 *
 * [LanguageResolver] がクライアントロケールの生文字列(`java.util.Locale`)を必ずこの有限な enum に
 * マップしてから [io.github.aktnb.playerStats.i18n.MessageCatalog] で対応する固定の [Messages] を
 * 選ぶことで、ロケールの生文字列がMapキーやファイルパスの動的結合に使われないようにしている。
 */
enum class Language {
    JA,
    EN,
}
