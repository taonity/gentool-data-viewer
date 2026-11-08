package org.taonity.gentooldataviewer.replay.repository

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Instant

data class ExpiredReplay(val id: String, val reporterId: String)
data class RetainedPlayerName(val playerId: String, val name: String, val count: Long, val latestAt: Instant)
data class RetainedPlayerSummary(val playerId: String, val mainName: String, val aliasesJson: String, val replayCount: Long)

@Repository
class ReplayRetentionRepository(private val jdbc: NamedParameterJdbcTemplate) {
    @Transactional(propagation = Propagation.MANDATORY)
    fun lock() {
        jdbc.queryForObject("SELECT id FROM replay_retention_lock WHERE id = 1 FOR UPDATE", emptyMap<String, Any>(), Int::class.java)
    }

    fun expiredHardware(cutoff: Instant, limit: Int): List<String> = jdbc.query(
        "SELECT player_id FROM player_hardware WHERE observed_at < :cutoff ORDER BY observed_at, player_id LIMIT :limit",
        mapOf("cutoff" to Timestamp.from(cutoff), "limit" to limit),
    ) { row, _ -> row.getString("player_id") }

    fun deleteHardware(ids: List<String>): Int = jdbc.update(
        "DELETE FROM player_hardware WHERE player_id IN (:ids)", mapOf("ids" to ids),
    )

    fun expiredReplays(cutoff: Instant, limit: Int): List<ExpiredReplay> = jdbc.query(
        "SELECT id, reporter_id FROM replay WHERE match_at < :cutoff ORDER BY match_at, id LIMIT :limit",
        mapOf("cutoff" to Timestamp.from(cutoff), "limit" to limit),
    ) { row, _ -> ExpiredReplay(row.getString("id"), row.getString("reporter_id")) }

    fun deleteSourceHardware(replayIds: List<String>): Int = jdbc.update(
        "DELETE FROM player_hardware WHERE source_replay_id IN (:ids)", mapOf("ids" to replayIds),
    )

    fun deleteReplays(ids: List<String>): Int = jdbc.update("DELETE FROM replay WHERE id IN (:ids)", mapOf("ids" to ids))

    fun retainedNames(playerIds: Set<String>, cutoff: Instant): List<RetainedPlayerName> = jdbc.query(
        """
        SELECT reporter_id, reporter_name, COUNT(*) AS games, MAX(match_at) AS latest_at
        FROM replay
        WHERE reporter_id IN (:ids) AND match_at >= :cutoff
          AND EXISTS (SELECT 1 FROM player_hardware h WHERE h.player_id = replay.reporter_id)
        GROUP BY reporter_id, reporter_name
        """.trimIndent(),
        mapOf("ids" to playerIds, "cutoff" to Timestamp.from(cutoff)),
    ) { row, _ ->
        RetainedPlayerName(row.getString("reporter_id"), row.getString("reporter_name"), row.getLong("games"),
            row.getTimestamp("latest_at").toInstant())
    }

    fun updateSummaries(summaries: List<RetainedPlayerSummary>) {
        if (summaries.isEmpty()) return
        jdbc.batchUpdate(
            "UPDATE player_hardware SET main_name = :name, aliases_json = :aliases, replay_count = :count WHERE player_id = :id",
            summaries.map { mapOf("id" to it.playerId, "name" to it.mainName, "aliases" to it.aliasesJson, "count" to it.replayCount) }.toTypedArray(),
        )
    }
}
