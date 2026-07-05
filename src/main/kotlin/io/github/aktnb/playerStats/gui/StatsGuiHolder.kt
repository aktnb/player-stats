package io.github.aktnb.playerStats.gui

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryHolder

/**
 * ステータス閲覧GUIであることを判定するためのマーカー用 [InventoryHolder]。
 *
 * `Bukkit.createInventory(holder, ...)` は holder インスタンスが先に必要なため、
 * インベントリ生成後に [inventory] へ設定する運用とし、[getInventory] はそれを返す。
 * イベント側では `event.view.topInventory.holder is StatsGuiHolder` で判定する。
 */
class StatsGuiHolder : InventoryHolder {
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
