package io.github.aktnb.playerStats.command

import io.github.aktnb.playerStats.gui.StatsGuiFactory
import io.github.aktnb.playerStats.repository.StatsRepository
import io.github.aktnb.playerStats.scheduler.PluginScheduler
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class StatsCommand(
    private val repository: StatsRepository,
    private val scheduler: PluginScheduler
) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (sender !is Player) {
            sender.sendMessage("This command can only be used by players.")
            return true
        }

        val player = sender
        val uuid = player.uniqueId

        scheduler.runAsync {
            try {
                val stats = repository.findByUuid(uuid)
                val blocksMined = stats?.blocksMined ?: 0L
                val blocksPlaced = stats?.blocksPlaced ?: 0L

                scheduler.runEntity(player) {
                    if (!player.isOnline) {
                        return@runEntity
                    }

                    val inventory = StatsGuiFactory.build(
                        targetName = player.name,
                        blocksMined = blocksMined,
                        blocksPlaced = blocksPlaced,
                    )
                    player.openInventory(inventory)
                }
            } catch (e: Exception) {
                e.printStackTrace()

                scheduler.runEntity(player) {
                    if (player.isOnline) {
                        player.sendMessage(
                            Component.text("統計データの取得に失敗しました。", NamedTextColor.RED)
                        )
                    }
                }
            }
        }

        return true
    }
}