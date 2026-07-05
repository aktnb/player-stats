package io.github.aktnb.playerStats.repository

import io.github.aktnb.playerStats.stats.PlayerStats
import io.github.aktnb.playerStats.stats.PlayerStatsDelta
import java.util.UUID
import kotlin.uuid.Uuid

class StatsRepository(
    private val sqlite: SQLiteProvider
) {
    fun init() {
        sqlite.getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(
                    """
                        CREATE TABLE IF NOT EXISTS player_stats (
                            player_uuid TEXT PRIMARY KEY,
                            player_name TEXT NOT NULL,
                            blocks_mined INTEGER NOT NULL DEFAULT 0,
                            blocks_placed INTEGER NOT NULL DEFAULT 0,
                            last_seen_at INTEGER NOT NULL
                        )
                    """.trimIndent()
                )
            }
        }
    }

    fun saveDeltas(deltas: List<PlayerStatsDelta>) {
        if (deltas.isEmpty()) return

        sqlite.getConnection().use { connection ->
            connection.autoCommit = false

            val sql = """
                INSERT INTO player_stats (
                    player_uuid,
                    player_name,
                    blocks_mined,
                    blocks_placed,
                    last_seen_at
                ) VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (player_uuid) DO UPDATE SET
                    player_name = excluded.player_name,
                    blocks_mined = player_stats.blocks_mined + excluded.blocks_mined,
                    blocks_placed = player_stats.blocks_placed + excluded.blocks_placed,
                    last_seen_at = excluded.last_seen_at
            """.trimIndent()

            connection.prepareStatement(sql).use { statement ->
                for (delta in deltas) {
                    statement.setString(1, delta.uuid.toString())
                    statement.setString(2, delta.name)
                    statement.setLong(3, delta.blockMined)
                    statement.setLong(4, delta.blockPlaced)
                    statement.setLong(5, delta.lastSeenAt)
                    statement.addBatch()
                }

                statement.executeBatch()
            }

            connection.commit()
        }
    }

    fun findByUuid(uuid: UUID): PlayerStats? {
        sqlite.getConnection().use { connection ->
            connection.prepareStatement(
                """
                    SELECT
                        player_uuid,
                        player_name,
                        blocks_mined,
                        blocks_placed,
                        last_seen_at
                    FROM player_stats
                    WHERE player_uuid = ?;
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, uuid.toString())

                statement.executeQuery().use { resultSet ->
                    if (!resultSet.next()) return null
                    return PlayerStats(
                        UUID.fromString(resultSet.getString("player_uuid")),
                        resultSet.getString("player_name"),
                        resultSet.getLong("blocks_mined"),
                        resultSet.getLong("blocks_placed"),
                        resultSet.getLong("last_seen_at"),
                    )
                }
            }

        }
    }
}