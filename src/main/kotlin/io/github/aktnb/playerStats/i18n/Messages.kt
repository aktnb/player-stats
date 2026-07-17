package io.github.aktnb.playerStats.i18n

import io.github.aktnb.playerStats.gui.StatDetailSort
import io.github.aktnb.playerStats.gui.StatDetailType
import io.github.aktnb.playerStats.stats.PlayTime
import io.github.aktnb.playerStats.stats.PlayTimeUnit

/**
 * GUI表示用の文言一式。[MessageCatalog] が [Language] ごとに1インスタンスだけ静的に保持し、
 * GUI構築のたびに再構築せず使い回す。
 *
 * サマリー画面の各アイテムの表示名(displayName)と、内訳画面のlore見出し(呼び出し側で
 * `"${label}: "` を組み立てる)は同じ [statLabel] を参照する。これにより「採掘数」(サマリー)と
 * 「採掘数: 」(detail)を別々に定義していた既存コードの二重管理を解消している。
 *
 * [statLabel]/[detailTitle]/[emptyLabel]/[sortLabel] はMapではなく関数として持たせている。
 * `Map<StatDetailType, String>` + `getValue()` 方式だと、将来 [StatDetailType]/[StatDetailSort] に
 * 列挙子を追加した際にMapへの追加を忘れてもコンパイルが通り、実行時に `NoSuchElementException` で
 * 初めて気づく形になってしまう。関数実装側で `when` 式を使い `else` 節を書かないことで、列挙子追加時に
 * 既存の `when` 式がコンパイルエラーになる(網羅性チェックが効く)ようにしている。これは
 * [io.github.aktnb.playerStats.gui.EntityIconResolver] が徹底している「例外を一切投げない確定的関数」
 * という設計方針とも一貫する。
 */
data class Messages(
    /** サマリー画面タイトルで、対象プレイヤー名に続けて表示する接尾辞(例: " のステータス")。 */
    val summaryTitleSuffix: String,
    /** サマリー画面の各アイテムの表示名、および内訳画面lore見出しの元になるラベルを返す。 */
    val statLabel: (StatDetailType) -> String,
    /** 内訳画面タイトルに使う種別名(例: "採掘内訳")を返す。 */
    val detailTitle: (StatDetailType) -> String,
    /** 内訳が空のときのプレースホルダーアイテムの表示名を返す。 */
    val emptyLabel: (StatDetailType) -> String,
    /** ソートボタンの表示名を返す。 */
    val sortLabel: (StatDetailSort) -> String,
    /** ソートボタンのlore: そのボタンが現在選択中のソートであることを示す文言。 */
    val sortStateActive: String,
    /** ソートボタンのlore: クリックでそのソートに切り替えられることを示す文言。 */
    val sortStateInactive: String,
    /** サマリー画面の累計プレイ時間アイテムの表示名、および同アイテムlore見出しの元になるラベル。 */
    val playTimeLabel: String,
    /**
     * 累計プレイ時間の各単位(日/時/分)のラベルを返す。`Map` ではなく網羅的 `when` 実装の関数として
     * 持たせることで、将来 [PlayTimeUnit] に列挙子を追加した際にコンパイルエラーで気づけるようにしている
     * ([statLabel] などと同じ設計方針)。
     */
    val playTimeUnitLabel: (PlayTimeUnit) -> String,
    /** 「戻る」ボタンの表示名。 */
    val navBack: String,
    /** 「前のページ」ボタンの表示名。 */
    val navPrevPage: String,
    /** 「次のページ」ボタンの表示名。 */
    val navNextPage: String,
    /** 対象プレイヤーがオフラインだった場合にプレイヤーへ送るエラーメッセージ。 */
    val errorTargetOffline: String,
    /** 統計データの取得に失敗した場合にプレイヤーへ送るエラーメッセージ。 */
    val errorStatsFetchFailed: String,
    /** 内訳画面タイトルで対象プレイヤー名の直後に置く接続語(例: " の" / "'s ")。 */
    private val detailTitleConnector: String,
) {
    /**
     * 内訳画面タイトルの、対象プレイヤー名に続けて表示する接尾辞を組み立てる。
     *
     * targetName自体はこの文字列に含まれない。呼び出し側は既存どおり [net.kyori.adventure.text.Component]
     * の合成(`Component.text().append()`)でtargetNameとこの接尾辞を連結すること
     * (targetNameを直接この関数やString.format等の文字列結合に渡さない)。
     */
    fun detailTitleSuffix(type: StatDetailType, currentPage: Int, totalPages: Int, sort: StatDetailSort): String =
        "$detailTitleConnector${detailTitle(type)} ($currentPage/$totalPages ${sortLabel(sort)})"

    /**
     * 累計プレイ時間を「日 時 分」形式の表示文字列に整形する。表示すべき単位の判断(上位/末尾ゼロ省略・
     * 全0時の最小単位表示)という言語非依存のロジックは [PlayTime.displayUnits] に委ね、本メソッドは
     * 各単位への言語依存ラベル付けと連結のみを担う([detailTitleSuffix] と同様に、言語依存部
     * ([playTimeUnitLabel]) は関数フィールドとして分離し、組み立ては共通ヘルパーに集約している)。
     *
     * 例(JA): "45分" / "3時間 20分" / "2日 5時間"。(EN): "45m" / "3h 20m" / "2d 5h"。
     */
    fun formatPlayTime(playTime: PlayTime): String =
        playTime.displayUnits().joinToString(" ") { "${it.value}${playTimeUnitLabel(it.unit)}" }
}

/**
 * [Language] ごとの [Messages] を提供するカタログ。
 *
 * JA/ENの文言データはリクエスト(GUI構築)のたびに再構築せず、`object` の `private val` として
 * 一度だけ静的に生成し使い回す。[forLanguage] は有限の [Language] enumのみを受け取り、
 * 対応する固定インスタンスを返すだけの純粋な選択関数であり、ロケールの生文字列を
 * Mapキー等に直接使うことはない。
 */
object MessageCatalog {
    private val JA = Messages(
        summaryTitleSuffix = " のステータス",
        statLabel = { type ->
            when (type) {
                StatDetailType.MINING -> "採掘数"
                StatDetailType.PLACEMENT -> "設置数"
                StatDetailType.MOB_KILL -> "キル数"
            }
        },
        detailTitle = { type ->
            when (type) {
                StatDetailType.MINING -> "採掘内訳"
                StatDetailType.PLACEMENT -> "設置内訳"
                StatDetailType.MOB_KILL -> "キル内訳"
            }
        },
        emptyLabel = { type ->
            when (type) {
                StatDetailType.MINING -> "採掘記録なし"
                StatDetailType.PLACEMENT -> "設置記録なし"
                StatDetailType.MOB_KILL -> "キル記録なし"
            }
        },
        sortLabel = { sort ->
            when (sort) {
                StatDetailSort.COUNT_ASC -> "昇順"
                StatDetailSort.COUNT_DESC -> "降順"
                StatDetailSort.NAME_ASC -> "辞書順"
                StatDetailSort.NAME_DESC -> "辞書逆順"
            }
        },
        sortStateActive = "現在の並び順",
        sortStateInactive = "クリックで並び替え",
        playTimeLabel = "累計プレイ時間",
        playTimeUnitLabel = { unit ->
            when (unit) {
                PlayTimeUnit.DAY -> "日"
                PlayTimeUnit.HOUR -> "時間"
                PlayTimeUnit.MINUTE -> "分"
            }
        },
        navBack = "戻る",
        navPrevPage = "◀ 前のページ",
        navNextPage = "次のページ ▶",
        errorTargetOffline = "対象プレイヤーはオフラインです。",
        errorStatsFetchFailed = "統計データの取得に失敗しました。",
        detailTitleConnector = " の",
    )

    private val EN = Messages(
        summaryTitleSuffix = "'s Stats",
        statLabel = { type ->
            when (type) {
                StatDetailType.MINING -> "Blocks Mined"
                StatDetailType.PLACEMENT -> "Blocks Placed"
                StatDetailType.MOB_KILL -> "Mob Kills"
            }
        },
        detailTitle = { type ->
            when (type) {
                StatDetailType.MINING -> "Mining Breakdown"
                StatDetailType.PLACEMENT -> "Placement Breakdown"
                StatDetailType.MOB_KILL -> "Kill Breakdown"
            }
        },
        emptyLabel = { type ->
            when (type) {
                StatDetailType.MINING -> "No mining records"
                StatDetailType.PLACEMENT -> "No placement records"
                StatDetailType.MOB_KILL -> "No kill records"
            }
        },
        sortLabel = { sort ->
            when (sort) {
                StatDetailSort.COUNT_ASC -> "Ascending"
                StatDetailSort.COUNT_DESC -> "Descending"
                StatDetailSort.NAME_ASC -> "Name A-Z"
                StatDetailSort.NAME_DESC -> "Name Z-A"
            }
        },
        sortStateActive = "Current sort",
        sortStateInactive = "Click to sort",
        playTimeLabel = "Playtime",
        playTimeUnitLabel = { unit ->
            when (unit) {
                PlayTimeUnit.DAY -> "d"
                PlayTimeUnit.HOUR -> "h"
                PlayTimeUnit.MINUTE -> "m"
            }
        },
        navBack = "Back",
        navPrevPage = "◀ Previous Page",
        navNextPage = "Next Page ▶",
        errorTargetOffline = "The target player is offline.",
        errorStatsFetchFailed = "Failed to fetch stats data.",
        detailTitleConnector = "'s ",
    )

    fun forLanguage(language: Language): Messages = when (language) {
        Language.JA -> JA
        Language.EN -> EN
    }
}
