package io.github.aktnb.playerStats.gui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack

/**
 * 別プレイヤーのステータスを閲覧するための固定GUI(サイズ9・1列)を組み立てる。
 *
 * ピッケル(採掘数)・草ブロック(設置数)の2アイテムのみを並べ、
 * 残りは装飾用のガラス板で埋めた閲覧専用インベントリを返す。
 */
object StatsGuiFactory {

    private const val PICKAXE_SLOT = 3
    private const val GRASS_SLOT = 5

    fun build(targetName: String, blocksMined: Long, blocksPlaced: Long): Inventory {
        val holder = StatsGuiHolder()
        val title = Component.text("$targetName のステータス", NamedTextColor.GOLD)
        val inventory = Bukkit.createInventory(holder, 9, title)
        holder.setInventory(inventory)

        val filler = createFiller()
        for (slot in 0 until inventory.size) {
            inventory.setItem(slot, filler)
        }

        inventory.setItem(
            PICKAXE_SLOT,
            createItem(
                material = Material.DIAMOND_PICKAXE,
                displayName = Component.text("採掘数", NamedTextColor.GOLD),
                loreLabel = "採掘数: ",
                value = blocksMined,
            )
        )
        inventory.setItem(
            GRASS_SLOT,
            createItem(
                material = Material.GRASS_BLOCK,
                displayName = Component.text("設置数", NamedTextColor.GOLD),
                loreLabel = "設置数: ",
                value = blocksPlaced,
            )
        )

        return inventory
    }

    private fun createItem(
        material: Material,
        displayName: Component,
        loreLabel: String,
        value: Long,
    ): ItemStack {
        val item = ItemStack(material)
        item.editMeta { meta ->
            meta.displayName(displayName.decoration(TextDecoration.ITALIC, false))
            meta.lore(
                listOf(
                    Component.text(loreLabel, NamedTextColor.GRAY)
                        .append(Component.text(value.toString(), NamedTextColor.WHITE))
                        .decoration(TextDecoration.ITALIC, false)
                )
            )
        }
        return item
    }

    private fun createFiller(): ItemStack {
        val item = ItemStack(Material.GRAY_STAINED_GLASS_PANE)
        item.editMeta { meta ->
            meta.displayName(
                Component.text(" ").decoration(TextDecoration.ITALIC, false)
            )
        }
        return item
    }
}
