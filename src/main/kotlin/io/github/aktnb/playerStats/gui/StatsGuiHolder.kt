package io.github.aktnb.playerStats.gui

import io.github.aktnb.playerStats.stats.MaterialMiningCount
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

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

/** 採掘数・設置数のサマリーを表示するGUIのholder。 */
class StatsSummaryGuiHolder(targetName: String) : StatsGuiHolder(targetName)

/**
 * ブロック別採掘内訳を表示するGUIのholder。
 *
 * [breakdown] はGUIを開いた時点のスナップショットをページ送りの間キャッシュとして使い回す
 * (ページ送りのたびにStatistic再読み取り・Foliaスレッドホップを発生させないため)。
 * そのため詳細GUIを開いている間に対象プレイヤーが採掘を続けると内容が古くなりうるが、
 * 表示時間の短いGUIなので許容する。「戻る」ボタンではサマリーを都度再取得する。
 */
class StatsMiningDetailGuiHolder(
    targetName: String,
    val breakdown: List<MaterialMiningCount>,
    val page: Int,
) : StatsGuiHolder(targetName)
