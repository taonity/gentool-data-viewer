package org.taonity.gentooldataviewer.cpu.repository

import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

@Repository
class CpuPlayerRankingRepository(
    private val jdbc: NamedParameterJdbcTemplate,
) {
    fun findRanks(
        playerIds: Collection<String>,
        sort: String?,
        direction: String?,
        linkedOnly: Boolean,
    ): Map<String, Long> {
        if (playerIds.isEmpty()) return emptyMap()
        val sortExpression = SORT_EXPRESSIONS[sort] ?: SORT_EXPRESSIONS.getValue("score")
        val sortDirection = if (direction == "asc") "ASC" else "DESC"
        val discordJoin = if (sort == "discordUser") {
            """
            LEFT JOIN gentool_user_link l
                ON l.player_id = h.player_id AND l.status = 'APPROVED'
            LEFT JOIN app_user u ON u.user_id = l.user_id
            """.trimIndent()
        } else {
            ""
        }
        val linkedFilter = if (linkedOnly) {
            """
            WHERE EXISTS (
                SELECT 1 FROM gentool_user_link linked
                WHERE linked.player_id = h.player_id AND linked.status = 'APPROVED'
            )
            """.trimIndent()
        } else {
            ""
        }
        val sql = """
            SELECT ranked.player_id, ranked.row_position
            FROM (
                SELECT h.player_id,
                       ROW_NUMBER() OVER (
                           ORDER BY CASE WHEN $sortExpression IS NULL THEN 1 ELSE 0 END,
                                    $sortExpression $sortDirection,
                                    h.main_name ASC,
                                    h.player_id ASC
                       ) AS row_position
                FROM player_hardware h
                $discordJoin
                $linkedFilter
            ) ranked
            WHERE ranked.player_id IN (:playerIds)
        """.trimIndent()
        return jdbc.query(sql, mapOf("playerIds" to playerIds)) { resultSet, _ ->
            resultSet.getString("player_id") to resultSet.getLong("row_position")
        }.toMap()
    }

    private companion object {
        val SORT_EXPRESSIONS = mapOf(
            "mainName" to "h.main_name",
            "discordUser" to "LOWER(u.display_name)",
            "playerId" to "h.player_id",
            "aliases" to "h.aliases_json",
            "replayCount" to "h.replay_count",
            "reportedCpu" to "h.cpu",
            "score" to "h.cpu_score",
            "latestName" to "h.latest_name",
            "benchmark" to "h.cpu_benchmark_name",
            "status" to "h.cpu_match_status",
            "observedAt" to "h.observed_at",
            "gentoolUpdatedAt" to "h.gentool_updated_at",
            "scoreUpdatedAt" to "h.cpu_score_updated_at",
        )
    }
}