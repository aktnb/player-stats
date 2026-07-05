package io.github.aktnb.playerStats.listener

import io.github.aktnb.playerStats.gui.StatsGuiFactory
import io.github.aktnb.playerStats.gui.StatsGuiHolder
import io.github.aktnb.playerStats.repository.StatsRepository
import io.github.aktnb.playerStats.scheduler.PluginScheduler
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
 * DBアクセスは [StatsCommand] と同じく `runAsync` → `runEntity` の二段構えで行い、
 * GUI内での一切の操作(クリック・ドラッグ)はキャンセルして閲覧専用にする。
 */
class StatsGuiListener(
    private val repository: StatsRepository,
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

        // 連打によるDB問い合わせ・GUI再オープンを抑制
        if (isOnCooldown(interactor.uniqueId)) return

        val targetUuid = target.uniqueId
        val targetName = target.name

        scheduler.runAsync {
            try {
                val stats = repository.findByUuid(targetUuid)
                val blocksMined = stats?.blocksMined ?: 0L
                val blocksPlaced = stats?.blocksPlaced ?: 0L

                scheduler.runEntity(interactor) {
                    if (!interactor.isOnline) {
                        return@runEntity
                    }

                    val inventory = StatsGuiFactory.build(
                        targetName = targetName,
                        blocksMined = blocksMined,
                        blocksPlaced = blocksPlaced,
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
