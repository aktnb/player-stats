package io.github.aktnb.playerStats.listener

import io.github.aktnb.playerStats.gui.StatDetailType
import io.github.aktnb.playerStats.gui.StatsDetailGuiHolder
import io.github.aktnb.playerStats.gui.StatsGuiFactory
import io.github.aktnb.playerStats.gui.StatsGuiHolder
import io.github.aktnb.playerStats.gui.StatsSummaryGuiHolder
import io.github.aktnb.playerStats.scheduler.PluginScheduler
import io.github.aktnb.playerStats.stats.VanillaStatsReader
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
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
 * サマリー画面のピッケル/草ブロックのクリックでそれぞれ採掘/設置のブロック別内訳画面へ、
 * 内訳画面の各ボタンでページ送り・サマリー復帰を行う。
 */
class StatsGuiListener(
    private val scheduler: PluginScheduler,
) : Listener {

    private val cooldowns = ConcurrentHashMap<UUID, Long>()

    /**
     * スレッドホップを伴う遷移(サマリー再取得・内訳取得)の処理中フラグ(開始時刻を保持)。多重発火を防ぐ。
     *
     * Folia環境では `runEntity(target)` にスケジュールしたタスクが、target退出時に実行されず破棄される
     * ことがあり、その場合 [markTransition] で登録したエントリの明示的除去が走らない。恒久ロックを避けるため
     * `cooldowns` と同様のタイムスタンプ方式とし、[TRANSITION_TIMEOUT_MILLIS] 経過したエントリは期限切れ
     * として無視する(明示除去が走らなくても一定時間後に自動復帰する)。
     */
    private val pendingTransitions = ConcurrentHashMap<UUID, Long>()

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

        openSummaryGui(targetName = target.name, viewer = interactor, expectedHolder = null)
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        cooldowns.remove(event.player.uniqueId)
        pendingTransitions.remove(event.player.uniqueId)
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val holder = event.view.topInventory.holder
        if (holder !is StatsGuiHolder) return
        event.isCancelled = true

        val viewer = event.whoClicked as? Player ?: return

        when (holder) {
            is StatsSummaryGuiHolder -> {
                when (event.rawSlot) {
                    StatsGuiFactory.PICKAXE_SLOT ->
                        openDetailGui(holder.targetName, viewer, StatDetailType.MINING, page = 0, expectedHolder = holder)
                    StatsGuiFactory.GRASS_SLOT ->
                        openDetailGui(holder.targetName, viewer, StatDetailType.PLACEMENT, page = 0, expectedHolder = holder)
                }
            }
            is StatsDetailGuiHolder -> {
                when (event.rawSlot) {
                    StatsGuiFactory.BACK_SLOT -> openSummaryGui(holder.targetName, viewer, expectedHolder = holder)
                    StatsGuiFactory.PREV_PAGE_SLOT -> {
                        if (holder.page > 0) {
                            scheduler.runEntity(viewer) {
                                if (viewer.isOnline && viewer.openInventory.topInventory.holder === holder) {
                                    viewer.openInventory(
                                        StatsGuiFactory.buildDetail(holder.targetName, holder.type, holder.breakdown, holder.page - 1)
                                    )
                                }
                            }
                        }
                    }
                    StatsGuiFactory.NEXT_PAGE_SLOT -> {
                        val maxPage = StatsGuiFactory.totalPages(holder.breakdown.size) - 1
                        if (holder.page < maxPage) {
                            scheduler.runEntity(viewer) {
                                if (viewer.isOnline && viewer.openInventory.topInventory.holder === holder) {
                                    viewer.openInventory(
                                        StatsGuiFactory.buildDetail(holder.targetName, holder.type, holder.breakdown, holder.page + 1)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @EventHandler
    fun onInventoryDrag(event: InventoryDragEvent) {
        if (event.view.topInventory.holder is StatsGuiHolder) {
            event.isCancelled = true
        }
    }

    private fun openSummaryGui(targetName: String, viewer: Player, expectedHolder: StatsGuiHolder?) {
        val target = Bukkit.getPlayerExact(targetName)
        if (target == null || !target.isOnline) {
            viewer.sendMessage(Component.text("対象プレイヤーはオフラインです。", NamedTextColor.RED))
            viewer.closeInventory()
            return
        }

        if (!markTransition(viewer.uniqueId)) {
            return
        }

        scheduler.runEntity(target) {
            try {
                if (!target.isOnline) {
                    clearTransition(viewer.uniqueId)
                    return@runEntity
                }
                val stats = VanillaStatsReader.read(target)
                val resolvedName = target.name

                scheduler.runEntity(viewer) {
                    try {
                        if (!viewer.isOnline) {
                            return@runEntity
                        }
                        if (expectedHolder != null && viewer.openInventory.topInventory.holder !== expectedHolder) {
                            return@runEntity
                        }
                        val inventory = StatsGuiFactory.build(
                            targetName = resolvedName,
                            blocksMined = stats.blocksMined,
                            blocksPlaced = stats.blocksPlaced,
                        )
                        viewer.openInventory(inventory)
                    } finally {
                        clearTransition(viewer.uniqueId)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                scheduler.runEntity(viewer) {
                    if (viewer.isOnline) {
                        viewer.sendMessage(Component.text("統計データの取得に失敗しました。", NamedTextColor.RED))
                    }
                    clearTransition(viewer.uniqueId)
                }
            }
        }
    }

    private fun openDetailGui(targetName: String, viewer: Player, type: StatDetailType, page: Int, expectedHolder: StatsGuiHolder?) {
        val target = Bukkit.getPlayerExact(targetName)
        if (target == null || !target.isOnline) {
            viewer.sendMessage(Component.text("対象プレイヤーはオフラインです。", NamedTextColor.RED))
            viewer.closeInventory()
            return
        }

        if (!markTransition(viewer.uniqueId)) {
            return
        }

        scheduler.runEntity(target) {
            try {
                if (!target.isOnline) {
                    clearTransition(viewer.uniqueId)
                    return@runEntity
                }
                val breakdown = when (type) {
                    StatDetailType.MINING -> VanillaStatsReader.readMiningBreakdown(target)
                    StatDetailType.PLACEMENT -> VanillaStatsReader.readPlacementBreakdown(target)
                }
                val resolvedName = target.name

                scheduler.runEntity(viewer) {
                    try {
                        if (!viewer.isOnline) {
                            return@runEntity
                        }
                        if (expectedHolder != null && viewer.openInventory.topInventory.holder !== expectedHolder) {
                            return@runEntity
                        }
                        viewer.openInventory(StatsGuiFactory.buildDetail(resolvedName, type, breakdown, page))
                    } finally {
                        clearTransition(viewer.uniqueId)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                scheduler.runEntity(viewer) {
                    if (viewer.isOnline) {
                        viewer.sendMessage(Component.text("統計データの取得に失敗しました。", NamedTextColor.RED))
                    }
                    clearTransition(viewer.uniqueId)
                }
            }
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

    /**
     * 遷移処理の開始を登録する。有効期限([TRANSITION_TIMEOUT_MILLIS])内の既存エントリがあれば
     * 処理中とみなし `false`(スキップ)を返す。期限切れ・未登録なら登録して `true` を返す。
     * Folia環境で `clearTransition` が走らなかった場合でも、期限切れ判定により自動復帰する。
     */
    private fun markTransition(uuid: UUID): Boolean {
        val now = System.currentTimeMillis()
        val last = pendingTransitions[uuid]
        if (last != null && now - last < TRANSITION_TIMEOUT_MILLIS) {
            return false
        }
        pendingTransitions[uuid] = now
        return true
    }

    private fun clearTransition(uuid: UUID) {
        pendingTransitions.remove(uuid)
    }

    private companion object {
        const val COOLDOWN_MILLIS = 800L
        const val TRANSITION_TIMEOUT_MILLIS = 1000L
    }
}
