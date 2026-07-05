package io.github.aktnb.playerStats.repository

import org.bukkit.plugin.java.JavaPlugin
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

class SQLiteProvider(
    private val plugin: JavaPlugin
) {
    private val dbFile: File = File(plugin.dataFolder, "stats.db")

    fun init() {
        plugin.dataFolder.mkdirs()

        Class.forName("org.sqlite.JDBC")

        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA journal_mode=WAL;")
                statement.execute("PRAGMA synchronous=NORMAL;")
                statement.execute("PRAGMA foreign_keys=ON;")
                statement.execute("PRAGMA busy_timeout=5000;")
            }
        }
    }

    fun getConnection(): Connection {
        return DriverManager.getConnection("jdbc:sqlite:${dbFile.absolutePath}")
    }
}