package org.taonity.gentooldataviewer.cpu.repository

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import org.taonity.gentooldataviewer.replay.dto.MatchDatePeriod
import java.time.Instant
import java.sql.Timestamp

data class PeriodPlayerName(
    val playerId: String,
    val name: String,
    val games: Long,
    val latestName: String,
    val latestReplayId: String,
    val observedAt: Instant,
    val cpu: String?,
    val gentoolUpdatedAt: Instant?,
    val linkedUserId: String?,
    val discordName: String?,
    val pictureUrl: String?,
)

@Repository
class CpuPlayerPeriodRepository(private val jdbc: NamedParameterJdbcTemplate) {
    fun aggregate(period: MatchDatePeriod): List<PeriodPlayerName> {
        val predicates = mutableListOf<String>()
        val parameters = mutableMapOf<String, Any>()
        period.from?.let {
            predicates += "match_at >= :fromDate"
            parameters["fromDate"] = Timestamp.from(it)
        }
        period.until?.let {
            predicates += "match_at < :untilDate"
            parameters["untilDate"] = Timestamp.from(it)
        }
        val where = if (predicates.isEmpty()) "" else "WHERE ${predicates.joinToString(" AND ")}"
        val sql = """
            WITH names AS (
                SELECT reporter_id, reporter_name,
                       COUNT(*) OVER (PARTITION BY reporter_id, reporter_name) AS games,
                       MAX(match_at) OVER (PARTITION BY reporter_id, reporter_name) AS last_named_at,
                       ROW_NUMBER() OVER (PARTITION BY reporter_id, reporter_name ORDER BY match_at DESC, id ASC) AS name_position,
                       FIRST_VALUE(reporter_name) OVER (PARTITION BY reporter_id ORDER BY match_at DESC, id ASC) AS latest_name,
                       FIRST_VALUE(id) OVER (PARTITION BY reporter_id ORDER BY match_at DESC, id ASC) AS latest_id,
                       FIRST_VALUE(cpu) OVER (PARTITION BY reporter_id ORDER BY match_at DESC, id ASC) AS latest_cpu,
                       MAX(match_at) OVER (PARTITION BY reporter_id) AS observed_at
                FROM replay $where
            )
            SELECT names.reporter_id, names.reporter_name, names.games,
                   names.latest_name, names.latest_id,
                   names.observed_at, names.latest_cpu, hardware.gentool_updated_at,
                   link.user_id, account.display_name, account.picture_url
            FROM names
            LEFT JOIN player_hardware hardware ON hardware.player_id = names.reporter_id
            LEFT JOIN gentool_user_link link ON link.player_id = names.reporter_id AND link.status = 'APPROVED'
            LEFT JOIN app_user account ON account.user_id = link.user_id
            WHERE names.name_position = 1
            ORDER BY names.reporter_id, names.games DESC, names.last_named_at DESC, names.reporter_name ASC
        """.trimIndent()
        return jdbc.query(sql, parameters) { row, _ ->
            PeriodPlayerName(
                playerId = row.getString("reporter_id"),
                name = row.getString("reporter_name"),
                games = row.getLong("games"),
                latestName = row.getString("latest_name"),
                latestReplayId = row.getString("latest_id"),
                observedAt = row.getTimestamp("observed_at").toInstant(),
                cpu = row.getString("latest_cpu"),
                gentoolUpdatedAt = row.getTimestamp("gentool_updated_at")?.toInstant(),
                linkedUserId = row.getString("user_id"),
                discordName = row.getString("display_name"),
                pictureUrl = row.getString("picture_url"),
            )
        }
    }
}