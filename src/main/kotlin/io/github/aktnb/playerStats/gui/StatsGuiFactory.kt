package io.github.aktnb.playerStats.gui

import io.github.aktnb.playerStats.stats.MaterialStatCount
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import kotlin.math.ceil
import kotlin.math.max

/**
 * 別プレイヤーのステータスを閲覧するための固定GUI(サイズ54)を組み立てる。
 *
 * サマリー画面ではピッケル(採掘数)・草ブロック(設置数)の2アイテムのみを配置し、残りのスロットは
 * 何も置かない閲覧専用インベントリを返す。ピッケル/草ブロッククリックで開くブロック別内訳画面
 * (採掘/設置・ページング付き)も本オブジェクトが組み立てる。
 */
object StatsGuiFactory {

    internal const val PICKAXE_SLOT = 21
    internal const val GRASS_SLOT = 23

    internal const val ITEMS_PER_PAGE = 45
    internal const val PREV_PAGE_SLOT = 45
    internal const val COUNT_ASC_SORT_SLOT = 46
    internal const val COUNT_DESC_SORT_SLOT = 47
    internal const val BACK_SLOT = 49
    internal const val NAME_ASC_SORT_SLOT = 51
    internal const val NAME_DESC_SORT_SLOT = 52
    internal const val NEXT_PAGE_SLOT = 53
    internal const val PLACEHOLDER_SLOT = 22

    /** 内訳エントリ数から総ページ数(最低1)を求める。空でも1ページ分は確保する。 */
    internal fun totalPages(size: Int): Int = max(1, ceil(size / ITEMS_PER_PAGE.toDouble()).toInt())

    fun build(targetName: String, blocksMined: Long, blocksPlaced: Long): Inventory {
        val holder = StatsSummaryGuiHolder(targetName)
        val title = Component.text("$targetName のステータス", NamedTextColor.GOLD)
        val inventory = Bukkit.createInventory(holder, 54, title)
        holder.setInventory(inventory)

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

    fun buildDetail(
        targetName: String,
        type: StatDetailType,
        breakdown: List<MaterialStatCount>,
        page: Int,
        sort: StatDetailSort = StatDetailSort.COUNT_DESC,
    ): Inventory {
        val sortedBreakdown = sortBreakdown(breakdown, sort)
        val totalPages = totalPages(breakdown.size)
        val clampedPage = page.coerceIn(0, totalPages - 1)

        val holder = StatsDetailGuiHolder(targetName, type, breakdown, clampedPage, sort)
        val title = Component.text(
            "$targetName の${type.titleLabel} (${clampedPage + 1}/$totalPages ${sort.label})",
            NamedTextColor.GOLD,
        )
        val inventory = Bukkit.createInventory(holder, 54, title)
        holder.setInventory(inventory)

        if (breakdown.isEmpty()) {
            inventory.setItem(PLACEHOLDER_SLOT, createPlaceholderItem(type.emptyLabel))
        } else {
            val fromIndex = clampedPage * ITEMS_PER_PAGE
            val toIndex = minOf(fromIndex + ITEMS_PER_PAGE, breakdown.size)
            for ((slot, entry) in sortedBreakdown.subList(fromIndex, toIndex).withIndex()) {
                inventory.setItem(slot, createBreakdownItem(entry.material, entry.count, type.loreLabel))
            }
        }

        if (clampedPage > 0) {
            inventory.setItem(PREV_PAGE_SLOT, createNavItem(Material.ARROW, "◀ 前のページ"))
        }
        if (clampedPage < totalPages - 1) {
            inventory.setItem(NEXT_PAGE_SLOT, createNavItem(Material.ARROW, "次のページ ▶"))
        }
        inventory.setItem(BACK_SLOT, createNavItem(Material.OAK_DOOR, "戻る"))
        inventory.setItem(
            COUNT_ASC_SORT_SLOT,
            createSortItem(Material.HOPPER, "昇順", sort == StatDetailSort.COUNT_ASC)
        )
        inventory.setItem(
            COUNT_DESC_SORT_SLOT,
            createSortItem(Material.CHEST, "降順", sort == StatDetailSort.COUNT_DESC)
        )
        inventory.setItem(
            NAME_ASC_SORT_SLOT,
            createSortItem(Material.NAME_TAG, "辞書順", sort == StatDetailSort.NAME_ASC)
        )
        inventory.setItem(
            NAME_DESC_SORT_SLOT,
            createSortItem(Material.WRITABLE_BOOK, "辞書逆順", sort == StatDetailSort.NAME_DESC)
        )

        return inventory
    }

    private fun sortBreakdown(breakdown: List<MaterialStatCount>, sort: StatDetailSort): List<MaterialStatCount> {
        val byName = compareBy<MaterialStatCount> { it.material.name }
        return when (sort) {
            StatDetailSort.COUNT_ASC -> breakdown.sortedWith(compareBy<MaterialStatCount> { it.count }.then(byName))
            StatDetailSort.COUNT_DESC -> breakdown.sortedWith(compareByDescending<MaterialStatCount> { it.count }.then(byName))
            StatDetailSort.NAME_ASC -> breakdown.sortedWith(byName.thenByDescending { it.count })
            StatDetailSort.NAME_DESC -> breakdown.sortedWith(byName.reversed().thenByDescending { it.count })
        }
    }

    private fun createBreakdownItem(material: Material, count: Long, loreLabel: String): ItemStack {
        val item = ItemStack(material)
        item.editMeta { meta ->
            meta.lore(
                listOf(
                    Component.text(loreLabel, NamedTextColor.GRAY)
                        .append(Component.text(count.toString(), NamedTextColor.WHITE))
                        .decoration(TextDecoration.ITALIC, false)
                )
            )
        }
        return item
    }

    private fun createPlaceholderItem(emptyLabel: String): ItemStack {
        val item = ItemStack(Material.BARRIER)
        item.editMeta { meta ->
            meta.displayName(
                Component.text(emptyLabel, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)
            )
        }
        return item
    }

    private fun createNavItem(material: Material, name: String): ItemStack {
        val item = ItemStack(material)
        item.editMeta { meta ->
            meta.displayName(Component.text(name, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false))
        }
        return item
    }

    private fun createSortItem(material: Material, name: String, active: Boolean): ItemStack {
        val item = ItemStack(material)
        item.editMeta { meta ->
            val color = if (active) NamedTextColor.GREEN else NamedTextColor.YELLOW
            meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false))
            meta.lore(
                listOf(
                    Component.text(if (active) "現在の並び順" else "クリックで並び替え", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
                )
            )
        }
        return item
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
}
