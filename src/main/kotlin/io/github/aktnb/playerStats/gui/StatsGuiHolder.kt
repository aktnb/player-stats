package io.github.aktnb.playerStats.gui

import io.github.aktnb.playerStats.stats.EntityStatCount
import io.github.aktnb.playerStats.stats.MaterialStatCount
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

/** 内訳の種別。表示文言をここに集約する。 */
enum class StatDetailType(val titleLabel: String, val loreLabel: String, val emptyLabel: String) {
    MINING("採掘内訳", "採掘数: ", "採掘記録なし"),
    PLACEMENT("設置内訳", "設置数: ", "設置記録なし"),
    MOB_KILL("キル内訳", "キル数: ", "キル記録なし"),
}

/** ブロック別内訳の並び順。 */
enum class StatDetailSort(val label: String) {
    COUNT_ASC("昇順"),
    COUNT_DESC("降順"),
    NAME_ASC("辞書順"),
    NAME_DESC("辞書逆順"),
}

/**
 * ステータス閲覧GUIであることを判定するためのマーカー用 [InventoryHolder]。
 *
 * `Bukkit.createInventory(holder, ...)` は holder インスタンスが先に必要なため、
 * インベントリ生成後に [inventory] へ設定する運用とし、[getInventory] はそれを返す。
 * イベント側では `event.view.topInventory.holder is StatsGuiHolder` で判定する(sealed親クラスへの
 * `is` チェックとしてサブクラス全てにマッチする)。
 */
sealed class StatsGuiHolder(val targetName: String) : InventoryHolder {
    // 注: `override lateinit var inventory: Inventory` は使えない。
    // InventoryHolder#getInventory() は Java メソッドであり、Kotlin はこれを
    // プロパティのオーバーライド対象として認識しない（"'inventory' overrides nothing"）。
    // 一方 `lateinit var inventory` + `override fun getInventory()` にすると、
    // プロパティが生成する getInventory() と衝突する（Platform declaration clash）。
    // そのためバッキングフィールドは別名にし、getInventory() を明示的にオーバーライドする。
    private lateinit var backingInventory: Inventory

    fun setInventory(inventory: Inventory) {
        backingInventory = inventory
    }

    override fun getInventory(): Inventory = backingInventory
}

/** 採掘数・設置数・キル数のサマリーを表示するGUIのholder。 */
class StatsSummaryGuiHolder(targetName: String) : StatsGuiHolder(targetName)

/**
 * 内訳(採掘/設置/キル)を表示するGUIのholder。[type] で種別を区別する。
 *
 * 内訳データはGUIを開いた時点のスナップショットをページ送りの間キャッシュとして使い回す
 * (ページ送りのたびにStatisticを再読み取りしないため)。
 * そのため詳細GUIを開いている間に対象プレイヤーが採掘/設置/キルを続けると内容が古くなりうるが、
 * 表示時間の短いGUIなので許容する。「戻る」ボタンではサマリーを都度再取得する。
 *
 * 内訳の要素型はブロック([MaterialStatCount])とエンティティ([EntityStatCount])で異なるため、
 * sealed 継承で [MaterialDetailGuiHolder] / [EntityDetailGuiHolder] に分岐する。両サブクラスとも
 * [StatsGuiHolder] を継承するため、`is StatsGuiHolder` 判定(先行キャンセルの不変条件)は維持される。
 * 共通のページ数計算のため、要素数 [breakdownSize] を抽象プロパティとして公開する。
 */
sealed class StatsDetailGuiHolder(
    targetName: String,
    val type: StatDetailType,
    val page: Int,
    val sort: StatDetailSort,
) : StatsGuiHolder(targetName) {
    abstract val breakdownSize: Int
}

/** ブロック別内訳(採掘/設置)を表示するGUIのholder。 */
class MaterialDetailGuiHolder(
    targetName: String,
    type: StatDetailType,
    val breakdown: List<MaterialStatCount>,
    page: Int,
    sort: StatDetailSort,
) : StatsDetailGuiHolder(targetName, type, page, sort) {
    override val breakdownSize: Int get() = breakdown.size
}

/** エンティティ別内訳(キル)を表示するGUIのholder。 */
class EntityDetailGuiHolder(
    targetName: String,
    type: StatDetailType,
    val breakdown: List<EntityStatCount>,
    page: Int,
    sort: StatDetailSort,
) : StatsDetailGuiHolder(targetName, type, page, sort) {
    override val breakdownSize: Int get() = breakdown.size
}
