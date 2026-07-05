package io.github.aktnb.playerStats.listener

import io.github.aktnb.playerStats.gui.StatsGuiFactory
import io.github.aktnb.playerStats.gui.StatsGuiHolder
import io.github.aktnb.playerStats.scheduler.PluginScheduler
import io.github.aktnb.playerStats.stats.VanillaStatsReader
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 別プレイヤーを右クリックすると、その相手のステータスを閲覧する専用GUIを開くリスナー。
 *
 * 統計の読み取りは対象プレイヤー(target)自身が所有するエンティティスレッドで行う必要がある
 * (Folia対応)ため、`runEntity(target)` で読み取ってから `runEntity(interactor)` へホップして
 * GUIを開く二段構えにしている。GUI内での一切の操作(クリック・ドラッグ)はキャンセルして閲覧専用にする。
 */
class StatsGuiListener(
    private val scheduler: PluginScheduler,
) : Listener {

    private val cooldowns = ConcurrentHashMap<UUID, Long>()

    @EventHandler(ignoreCancelled = true)
    fun onInteract(event: PlayerInteractEntityEvent) {
        val target = event.rightClicked
        if (target !is Player) return

        // オフハンド分の重複発火を防ぐ
        if (event.hand != EquipmentSlot.HAND) return

        val interactor = event.player

        // GUIを開く通常時と連打時とで挙動が食い違わないよう、右クリックの既定動作は
        // クールダウン判定より前に必ずキャンセルする。
        event.isCancelled = true

        // 連打によるGUI再オープンを抑制
        if (isOnCooldown(interactor.uniqueId)) return

        scheduler.runEntity(target) {
            if (!target.isOnline) {
                return@runEntity
            }

            try {
                val stats = VanillaStatsReader.read(target)
                val targetName = target.name

                scheduler.runEntity(interactor) {
                    if (!interactor.isOnline) {
                        return@runEntity
                    }

                    val inventory = StatsGuiFactory.build(
                        targetName = targetName,
                        blocksMined = stats.blocksMined,
                        blocksPlaced = stats.blocksPlaced,
                    )
                    interactor.openInventory(inventory)
                }
            } catch (e: Exception) {
                e.printStackTrace()

                scheduler.runEntity(interactor) {
                    if (interactor.isOnline) {
                        interactor.sendMessage(
                            Component.text("統計データの取得に失敗しました。", NamedTextColor.RED)
                        )
                    }
                }
            }
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        cooldowns.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        if (event.view.topInventory.holder is StatsGuiHolder) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.holder is StatsGuiHolder) {
            event.isCancelled = true
        }
    }

    private fun isOnCooldown(uuid: UUID): Boolean {
        val now = System.currentTimeMillis()
        val last = cooldowns[uuid]
        if (last != null && now - last < COOLDOWN_MILLIS) {
            return true
        }
        cooldowns[uuid] = now
        return false
    }

    private companion object {
        const val COOLDOWN_MILLIS = 800L
    }
}
