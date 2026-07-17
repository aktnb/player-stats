package io.github.aktnb.playerStats.gui

import io.github.aktnb.playerStats.i18n.Messages
import io.github.aktnb.playerStats.stats.EntityStatCount
import io.github.aktnb.playerStats.stats.MaterialStatCount
import io.github.aktnb.playerStats.stats.PlayTime
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
 * サマリー画面ではピッケル(採掘数)・草ブロック(設置数)・鉄の剣(キル数)の3アイテムを対称配置し、
 * 残りのスロットは何も置かない閲覧専用インベントリを返す。各アイテムのクリックで開く内訳画面
 * (採掘/設置/キル・ページング付き)も本オブジェクトが組み立てる。
 */
object StatsGuiFactory {

    private val PLAYER_NAME_TITLE_COLOR = NamedTextColor.DARK_AQUA

    // サマリー画面(row2 = slot18-26)に4アイテムを対称配置する(19/21/23/25)。
    internal const val PICKAXE_SLOT = 19
    internal const val GRASS_SLOT = 21
    internal const val SWORD_SLOT = 23
    internal const val CLOCK_SLOT = 25

    internal const val ITEMS_PER_PAGE = 45
    internal const val PREV_PAGE_SLOT = 45
    internal const val COUNT_ASC_SORT_SLOT = 46
    internal const val COUNT_DESC_SORT_SLOT = 47
    internal const val BACK_SLOT = 49
    internal const val NAME_ASC_SORT_SLOT = 51
    internal const val NAME_DESC_SORT_SLOT = 52
    internal const val NEXT_PAGE_SLOT = 53

    /**
     * 内訳画面が空(記録なし)のときにプレースホルダーを置くスロット。サマリー画面のアイテムスロットとは
     * 別インベントリなので値自体は任意だが、row2中央(22)に置くことで見栄えを揃える。かつては
     * [GRASS_SLOT] の値を暗黙に流用していたが、サマリー側のスロット再配置と独立に管理できるよう
     * 独立した定数として明示的に分離している。
     */
    internal const val PLACEHOLDER_SLOT = 22

    /** 内訳エントリ数から総ページ数(最低1)を求める。空でも1ページ分は確保する。 */
    internal fun totalPages(size: Int): Int = max(1, ceil(size / ITEMS_PER_PAGE.toDouble()).toInt())

    fun build(targetName: String, blocksMined: Long, blocksPlaced: Long, mobKills: Long, playTimeTicks: Long, messages: Messages): Inventory {
        val holder = StatsSummaryGuiHolder(targetName)
        val title = createTitle(targetName, messages.summaryTitleSuffix)
        val inventory = Bukkit.createInventory(holder, 54, title)
        holder.setInventory(inventory)

        inventory.setItem(
            PICKAXE_SLOT,
            createItem(
                material = Material.DIAMOND_PICKAXE,
                displayName = Component.text(messages.statLabel(StatDetailType.MINING), NamedTextColor.GOLD),
                loreLabel = "${messages.statLabel(StatDetailType.MINING)}: ",
                value = blocksMined,
            )
        )
        inventory.setItem(
            GRASS_SLOT,
            createItem(
                material = Material.GRASS_BLOCK,
                displayName = Component.text(messages.statLabel(StatDetailType.PLACEMENT), NamedTextColor.GOLD),
                loreLabel = "${messages.statLabel(StatDetailType.PLACEMENT)}: ",
                value = blocksPlaced,
            )
        )
        inventory.setItem(
            SWORD_SLOT,
            createItem(
                material = Material.IRON_SWORD,
                displayName = Component.text(messages.statLabel(StatDetailType.MOB_KILL), NamedTextColor.GOLD),
                loreLabel = "${messages.statLabel(StatDetailType.MOB_KILL)}: ",
                value = mobKills,
            )
        )
        inventory.setItem(
            CLOCK_SLOT,
            createTextValueItem(
                material = Material.CLOCK,
                displayName = Component.text(messages.playTimeLabel, NamedTextColor.GOLD),
                loreLabel = "${messages.playTimeLabel}: ",
                value = messages.formatPlayTime(PlayTime.fromTicks(playTimeTicks)),
            )
        )

        return inventory
    }

    /** ブロック別内訳(採掘/設置)画面を組み立てる。 */
    fun buildDetail(
        targetName: String,
        type: StatDetailType,
        breakdown: List<MaterialStatCount>,
        page: Int,
        messages: Messages,
        sort: StatDetailSort = StatDetailSort.COUNT_DESC,
    ): Inventory {
        val sortedBreakdown = sortMaterialBreakdown(breakdown, sort)
        val holder = MaterialDetailGuiHolder(targetName, type, breakdown, clampPage(page, breakdown.size), sort)
        return renderDetail(holder, messages) { globalIndex ->
            val entry = sortedBreakdown[globalIndex]
            createBreakdownItem(entry.material, entry.count, "${messages.statLabel(type)}: ")
        }
    }

    /**
     * エンティティ別内訳(キル)画面を組み立てる。
     *
     * [List] の型引数はJVMのシグネチャ上は消去されて Material 版と衝突するため、[JvmName] でJVM側の
     * メソッド名を分離する。Kotlin呼び出し側では引数の静的型でオーバーロードが解決されるため通常どおり
     * `buildDetail(...)` で呼べる。
     */
    @JvmName("buildEntityDetail")
    fun buildDetail(
        targetName: String,
        type: StatDetailType,
        breakdown: List<EntityStatCount>,
        page: Int,
        messages: Messages,
        sort: StatDetailSort = StatDetailSort.COUNT_DESC,
    ): Inventory {
        val sortedBreakdown = sortEntityBreakdown(breakdown, sort)
        val holder = EntityDetailGuiHolder(targetName, type, breakdown, clampPage(page, breakdown.size), sort)
        return renderDetail(holder, messages) { globalIndex ->
            val entry = sortedBreakdown[globalIndex]
            createBreakdownItem(
                material = EntityIconResolver.iconFor(entry.entityType),
                count = entry.count,
                loreLabel = "${messages.statLabel(type)}: ",
                // アイコンはスポーンエッグのままだが、表示名はエンティティ自体の翻訳キーを使うことで
                // 「〇〇のスポーンエッグ」ではなくMob名(クライアントの言語設定に応じてローカライズされる)を表示する。
                displayName = Component.translatable(entry.entityType, NamedTextColor.GOLD),
            )
        }
    }

    /** 内訳エントリ数から表示ページを0..(総ページ数-1)にクランプする。 */
    private fun clampPage(page: Int, size: Int): Int = page.coerceIn(0, totalPages(size) - 1)

    /**
     * 内訳画面の共通描画(ページング・ナビ・ソートボタン・空表示)を担う。要素型に依存しない部分を集約し、
     * アイテム生成のみ [itemFactory](対象ページ内グローバルインデックス→ItemStack)で差し込む。
     * ページ範囲・ソートは既に反映済みの [holder] を用いるため、呼び出し側でクランプ済みのholderを渡すこと。
     */
    private fun renderDetail(holder: StatsDetailGuiHolder, messages: Messages, itemFactory: (Int) -> ItemStack): Inventory {
        val type = holder.type
        val sort = holder.sort
        val clampedPage = holder.page
        val breakdownSize = holder.breakdownSize
        val totalPages = totalPages(breakdownSize)

        val title = createTitle(holder.targetName, messages.detailTitleSuffix(type, clampedPage + 1, totalPages, sort))
        val inventory = Bukkit.createInventory(holder, 54, title)
        holder.setInventory(inventory)

        if (breakdownSize == 0) {
            inventory.setItem(PLACEHOLDER_SLOT, createPlaceholderItem(messages.emptyLabel(type)))
        } else {
            val fromIndex = clampedPage * ITEMS_PER_PAGE
            val toIndex = minOf(fromIndex + ITEMS_PER_PAGE, breakdownSize)
            for (slot in 0 until (toIndex - fromIndex)) {
                inventory.setItem(slot, itemFactory(fromIndex + slot))
            }
        }

        if (clampedPage > 0) {
            inventory.setItem(PREV_PAGE_SLOT, createNavItem(Material.ARROW, messages.navPrevPage))
        }
        if (clampedPage < totalPages - 1) {
            inventory.setItem(NEXT_PAGE_SLOT, createNavItem(Material.ARROW, messages.navNextPage))
        }
        inventory.setItem(BACK_SLOT, createNavItem(Material.OAK_DOOR, messages.navBack))
        inventory.setItem(
            COUNT_ASC_SORT_SLOT,
            createSortItem(
                Material.HOPPER,
                messages.sortLabel(StatDetailSort.COUNT_ASC),
                sort == StatDetailSort.COUNT_ASC,
                messages,
            )
        )
        inventory.setItem(
            COUNT_DESC_SORT_SLOT,
            createSortItem(
                Material.CHEST,
                messages.sortLabel(StatDetailSort.COUNT_DESC),
                sort == StatDetailSort.COUNT_DESC,
                messages,
            )
        )
        inventory.setItem(
            NAME_ASC_SORT_SLOT,
            createSortItem(
                Material.NAME_TAG,
                messages.sortLabel(StatDetailSort.NAME_ASC),
                sort == StatDetailSort.NAME_ASC,
                messages,
            )
        )
        inventory.setItem(
            NAME_DESC_SORT_SLOT,
            createSortItem(
                Material.WRITABLE_BOOK,
                messages.sortLabel(StatDetailSort.NAME_DESC),
                sort == StatDetailSort.NAME_DESC,
                messages,
            )
        )

        return inventory
    }

    private fun sortMaterialBreakdown(breakdown: List<MaterialStatCount>, sort: StatDetailSort): List<MaterialStatCount> {
        val byName = compareBy<MaterialStatCount> { it.material.name }
        return when (sort) {
            StatDetailSort.COUNT_ASC -> breakdown.sortedWith(compareBy<MaterialStatCount> { it.count }.then(byName))
            StatDetailSort.COUNT_DESC -> breakdown.sortedWith(compareByDescending<MaterialStatCount> { it.count }.then(byName))
            StatDetailSort.NAME_ASC -> breakdown.sortedWith(byName.thenByDescending { it.count })
            StatDetailSort.NAME_DESC -> breakdown.sortedWith(byName.reversed().thenByDescending { it.count })
        }
    }

    private fun sortEntityBreakdown(breakdown: List<EntityStatCount>, sort: StatDetailSort): List<EntityStatCount> {
        val byName = compareBy<EntityStatCount> { it.entityType.name }
        return when (sort) {
            StatDetailSort.COUNT_ASC -> breakdown.sortedWith(compareBy<EntityStatCount> { it.count }.then(byName))
            StatDetailSort.COUNT_DESC -> breakdown.sortedWith(compareByDescending<EntityStatCount> { it.count }.then(byName))
            StatDetailSort.NAME_ASC -> breakdown.sortedWith(byName.thenByDescending { it.count })
            StatDetailSort.NAME_DESC -> breakdown.sortedWith(byName.reversed().thenByDescending { it.count })
        }
    }

    private fun createTitle(targetName: String, suffix: String): Component =
        Component.empty()
            .append(Component.text(targetName, PLAYER_NAME_TITLE_COLOR))
            .append(Component.text(suffix))

    /**
     * [displayName] を渡した場合はアイテムの表示名を明示的に上書きする(未指定ならMaterial自身の既定名のまま)。
     * ブロック内訳ではMaterial名がそのままブロック名なので上書き不要だが、エンティティ内訳では
     * アイコンにスポーンエッグを使う一方、表示名はMob名にしたいためこのオーバーロードを使う。
     */
    private fun createBreakdownItem(material: Material, count: Long, loreLabel: String, displayName: Component? = null): ItemStack {
        val item = ItemStack(material)
        item.editMeta { meta ->
            if (displayName != null) {
                meta.displayName(displayName.decoration(TextDecoration.ITALIC, false))
            }
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

    private fun createSortItem(material: Material, name: String, active: Boolean, messages: Messages): ItemStack {
        val item = ItemStack(material)
        item.editMeta { meta ->
            val color = if (active) NamedTextColor.GREEN else NamedTextColor.YELLOW
            meta.displayName(Component.text(name, color).decoration(TextDecoration.ITALIC, false))
            meta.lore(
                listOf(
                    Component.text(
                        if (active) messages.sortStateActive else messages.sortStateInactive,
                        NamedTextColor.GRAY,
                    ).decoration(TextDecoration.ITALIC, false)
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
    ): ItemStack = createTextValueItem(material, displayName, loreLabel, value.toString())

    /**
     * [createItem] の値が数値でない(整形済み文字列)版。累計プレイ時間のように「2日 5時間」といった
     * すでに整形済みの表示文字列をloreの値として出すために使う。数値版 [createItem] はこの実装に委譲する。
     */
    private fun createTextValueItem(
        material: Material,
        displayName: Component,
        loreLabel: String,
        value: String,
    ): ItemStack {
        val item = ItemStack(material)
        item.editMeta { meta ->
            meta.displayName(displayName.decoration(TextDecoration.ITALIC, false))
            meta.lore(
                listOf(
                    Component.text(loreLabel, NamedTextColor.GRAY)
                        .append(Component.text(value, NamedTextColor.WHITE))
                        .decoration(TextDecoration.ITALIC, false)
                )
            )
        }
        return item
    }
}
