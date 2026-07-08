package io.github.aktnb.playerStats.gui

import org.bukkit.Material
import org.bukkit.entity.EntityType

/**
 * [EntityType] からGUIアイコン用の [Material] を解決する確定的関数を提供する。
 *
 * 例外は一切投げず、常にアイテム化可能な有効な [Material] を返す。解決順は以下の3段構え:
 * 1. `<TYPE>_SPAWN_EGG`(スポーンエッグ)を [Material.getMaterial]（null許容）で探す。
 *    `Material.valueOf` ではなく `getMaterial` を使うことで、存在しない名前でも例外にならない。
 * 2. スポーンエッグが存在しない一部のMob([fallbackIcons])は個別に代替アイコンを割り当てる。
 * 3. いずれにも該当しない場合は既定の [Material.IRON_SWORD] を返す。
 *
 * 返す [Material] は全て非AIRかつアイテム化可能なので、そのまま `ItemStack` 生成に渡して安全。
 */
object EntityIconResolver {

    /** スポーンエッグが存在しないMobの代替アイコン。 */
    private val fallbackIcons: Map<EntityType, Material> = mapOf(
        EntityType.GIANT to Material.ZOMBIE_HEAD,
        EntityType.ILLUSIONER to Material.BOW,
    )

    fun iconFor(type: EntityType): Material {
        Material.getMaterial("${type.name}_SPAWN_EGG")?.let { return it }
        fallbackIcons[type]?.let { return it }
        return Material.IRON_SWORD
    }
}
