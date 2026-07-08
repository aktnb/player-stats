package io.github.aktnb.playerStats.command

import io.github.aktnb.playerStats.gui.StatsGuiFactory
import io.github.aktnb.playerStats.stats.VanillaStatsReader
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

class StatsCommand(
    private val plugin: Plugin
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

        Bukkit.getScheduler().runTask(plugin, Runnable {
            if (!player.isOnline) {
                return@Runnable
            }

            try {
                val stats = VanillaStatsReader.read(player)

                val inventory = StatsGuiFactory.build(
                    targetName = player.name,
                    blocksMined = stats.blocksMined,
                    blocksPlaced = stats.blocksPlaced,
                    mobKills = stats.mobKills,
                )
                player.openInventory(inventory)
            } catch (e: Exception) {
                // Defensive fallback: VanillaStatsReader only throws on truly unexpected
                // errors (its own IllegalArgumentException cases are already handled),
                // and openInventory/Adventure calls could theoretically fail too.
                e.printStackTrace()
                player.sendMessage(
                    Component.text("統計データの取得に失敗しました。", NamedTextColor.RED)
                )
            }
        })

        return true
    }
}
