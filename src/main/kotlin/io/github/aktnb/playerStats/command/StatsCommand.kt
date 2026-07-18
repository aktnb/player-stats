package io.github.aktnb.playerStats.command

import io.github.aktnb.playerStats.gui.StatsGuiFactory
import io.github.aktnb.playerStats.i18n.LanguageResolver
import io.github.aktnb.playerStats.i18n.MessageCatalog
import io.github.aktnb.playerStats.i18n.Messages
import io.github.aktnb.playerStats.stats.VanillaStatsReader
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import java.util.Locale

class StatsCommand(
    private val plugin: Plugin
) : CommandExecutor, TabCompleter {

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

                // GUIを構成する複数のComponent(タイトル・各アイテム名・lore)すべてで同一の
                // Messagesインスタンスを使い回すため、GUI構築直前のこの一箇所でのみ解決する。
                val messages = messagesFor(player.locale())

                val inventory = StatsGuiFactory.build(
                    targetName = player.name,
                    blocksMined = stats.blocksMined,
                    blocksPlaced = stats.blocksPlaced,
                    mobKills = stats.mobKills,
                    playTimeTicks = stats.playTimeTicks,
                    messages = messages,
                )
                player.openInventory(inventory)
            } catch (e: Exception) {
                // Defensive fallback: VanillaStatsReader only throws on truly unexpected
                // errors (its own IllegalArgumentException cases are already handled),
                // and openInventory/Adventure calls could theoretically fail too.
                e.printStackTrace()
                val locale = if (player.isOnline) player.locale() else Locale.US
                val messages = messagesFor(locale)
                player.sendMessage(
                    Component.text(messages.errorStatsFetchFailed, NamedTextColor.RED)
                )
            }
        })

        return true
    }

    /**
     * `/stats` は引数を取らないコマンド(自分の統計のみ表示。他プレイヤーの統計は
     * 右クリックで閲覧する設計)のため、常に空の候補を返す。未登録の場合の
     * Bukkitデフォルト挙動(オンラインプレイヤー名を補完してしまう)を防ぐ。
     */
    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> = emptyList()

    /**
     * [locale] から表示言語を解決し、対応する [Messages] を返す。
     * `Player.locale()` はPaper APIの公開ドキュメント上、明示的なnull安全性アノテーションは
     * 無い(Kotlin側ではplatform typeとして扱われる)が、実運用上nullが返るケースは
     * 確認されていないため非nullとして扱っている。
     */
    private fun messagesFor(locale: Locale): Messages =
        MessageCatalog.forLanguage(LanguageResolver.resolve(locale))
}
