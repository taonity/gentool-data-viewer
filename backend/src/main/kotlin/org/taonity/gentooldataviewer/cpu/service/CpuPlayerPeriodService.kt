package org.taonity.gentooldataviewer.cpu.service

import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.console.dto.PageResponse
import org.taonity.gentooldataviewer.cpu.dto.CpuPlayerDto
import org.taonity.gentooldataviewer.cpu.dto.CpuPlayerLatestMatchDto
import org.taonity.gentooldataviewer.cpu.dto.CpuPlayerSummaryDto
import org.taonity.gentooldataviewer.cpu.dto.DiscordUserDto
import org.taonity.gentooldataviewer.cpu.dto.RankedCpuPlayerDto
import org.taonity.gentooldataviewer.cpu.repository.CpuBenchmarkRepository
import org.taonity.gentooldataviewer.cpu.repository.CpuPlayerPeriodRepository
import org.taonity.gentooldataviewer.replay.dto.MatchDatePeriod
import org.taonity.gentooldataviewer.replay.repository.ReplayPlayerRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRepository
import tools.jackson.databind.ObjectMapper

@Service
@Transactional(readOnly = true)
class CpuPlayerPeriodService(
    private val repository: CpuPlayerPeriodRepository,
    private val ratingService: CpuRatingService,
    private val benchmarks: CpuBenchmarkRepository,
    private val benchmarkSync: CpuBenchmarkSyncService,
    private val replayPlayers: ReplayPlayerRepository,
    private val replays: ReplayRepository,
    private val settings: AppSettings,
    private val objectMapper: ObjectMapper,
) {
    private data class PeriodPlayer(val player: CpuPlayerDto, val replayId: String, val userId: String?)

    fun list(
        period: MatchDatePeriod, query: String?, field: String?, page: Int, size: Int,
        sort: String?, direction: String?, linkedOnly: Boolean, playerId: String?, exact: Boolean,
    ): PageResponse<CpuPlayerDto> {
        val target = playerId?.trim()?.uppercase()?.takeIf(String::isNotEmpty)
        val filtered = ordered(period, sort, direction, linkedOnly)
            .filter { target == null || it.player.playerId == target }
            .filter { matches(it.player, query, field, exact) }
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, settings.console().maxPageSize))
        val content = filtered.drop(pageable.offset.coerceAtMost(filtered.size.toLong()).toInt()).take(pageable.pageSize)
        val enriched = enrich(content)
        return PageResponse.of(PageImpl(enriched, pageable, filtered.size.toLong())) { it }
    }

    fun linked(period: MatchDatePeriod, userId: String, sort: String?, direction: String?, linkedOnly: Boolean): List<RankedCpuPlayerDto> {
        val ranked = ordered(period, sort, direction, linkedOnly).mapIndexedNotNull { index, row ->
            if (row.userId == userId) index + 1L to row else null
        }
        return ranked.map { it.first }.zip(enrich(ranked.map { it.second })) { rank, player -> RankedCpuPlayerDto(rank, player) }
    }

    fun summary(period: MatchDatePeriod): CpuPlayerSummaryDto {
        val players = aggregate(period).map { it.player }
        return CpuPlayerSummaryDto(
            totalPlayers = players.size.toLong(),
            ratedPlayers = players.count { it.singleThreadScore != null }.toLong(),
            unmatchedPlayers = players.count { it.matchStatus == CpuMatchStatus.UNMATCHED }.toLong(),
            ambiguousPlayers = players.count { it.matchStatus == CpuMatchStatus.AMBIGUOUS }.toLong(),
            playersWithoutCpu = players.count { it.matchStatus == CpuMatchStatus.NO_CPU }.toLong(),
            catalogEntries = benchmarks.count(),
            catalogFetchedAt = benchmarkSync.latestFetchedAt(),
            dataSince = replays.findEarliestCollectedAt(),
        )
    }

    private fun aggregate(period: MatchDatePeriod): List<PeriodPlayer> {
        val rows = repository.aggregate(period)
        val ratings = rows.map { it.cpu }.distinct().associateWith { ratingService.preview(it) }
        val catalogAt = benchmarkSync.latestFetchedAt()
        return rows.groupBy { it.playerId }.map { (playerId, names) ->
            val latest = names.first()
            val match = ratings.getValue(latest.cpu)
            PeriodPlayer(
                CpuPlayerDto(
                    playerId = playerId,
                    mainName = names.first().name,
                    discordUser = latest.discordName?.let { DiscordUserDto(it, latest.pictureUrl) },
                    latestName = latest.latestName,
                    aliases = names.drop(1).map { it.name },
                    replayCount = names.sumOf { it.games },
                    reportedCpu = latest.cpu,
                    singleThreadScore = match.benchmark?.singleThreadScore,
                    benchmarkModel = match.benchmark?.modelName,
                    benchmarkUrl = match.benchmark?.sourceUrl,
                    matchStatus = match.status,
                    observedAt = latest.observedAt,
                    gentoolUpdatedAt = latest.gentoolUpdatedAt,
                    scoreUpdatedAt = catalogAt.takeIf { match.benchmark != null },
                    latestMatch = null,
                ),
                latest.latestReplayId,
                latest.linkedUserId,
            )
        }
    }

    private fun ordered(period: MatchDatePeriod, sort: String?, direction: String?, linkedOnly: Boolean): List<PeriodPlayer> {
        val key = sort?.takeIf { it in CPU_PLAYER_SORT_PROPERTIES } ?: "score"
        val base = compareBy<PeriodPlayer, Comparable<Any>?>(nullsLast(naturalOrder())) { sortValue(it.player, key) }
        val primary = if (direction == "asc") base else Comparator<PeriodPlayer> { left, right ->
            val leftValue = sortValue(left.player, key)
            val rightValue = sortValue(right.player, key)
            when {
                leftValue == null -> if (rightValue == null) 0 else 1
                rightValue == null -> -1
                else -> rightValue.compareTo(leftValue)
            }
        }
        return aggregate(period).filter { !linkedOnly || it.userId != null }
            .sortedWith(primary.thenBy { if (key == "discordUser") it.player.mainName.lowercase() else it.player.mainName }
                .thenBy { it.player.playerId })
    }

    @Suppress("UNCHECKED_CAST")
    private fun sortValue(player: CpuPlayerDto, key: String): Comparable<Any>? = when (key) {
        "replayCount" -> player.replayCount
        "score" -> player.singleThreadScore
        "observedAt" -> player.observedAt
        "gentoolUpdatedAt" -> player.gentoolUpdatedAt
        "scoreUpdatedAt" -> player.scoreUpdatedAt
        "discordUser" -> player.discordUser?.displayName?.lowercase()
        "aliases" -> objectMapper.writeValueAsString(player.aliases)
        else -> values(player)[key]?.firstOrNull()
    } as Comparable<Any>?

    private fun matches(player: CpuPlayerDto, query: String?, field: String?, exact: Boolean): Boolean {
        val value = query?.trim()?.takeIf(String::isNotEmpty) ?: return true
        val fields = values(player)
        val candidates = if (field.isNullOrBlank() || field == "all") fields.values.flatten() else fields[field].orEmpty()
        return candidates.any { if (exact) it.equals(value, ignoreCase = true) else it.contains(value, ignoreCase = true) }
    }

    private fun values(player: CpuPlayerDto): Map<String, List<String>> = mapOf(
        "mainName" to listOf(player.mainName), "playerId" to listOf(player.playerId), "aliases" to player.aliases,
        "latestName" to listOf(player.latestName), "replayCount" to listOf(player.replayCount.toString()),
        "reportedCpu" to listOfNotNull(player.reportedCpu), "score" to listOfNotNull(player.singleThreadScore?.toString()),
        "benchmark" to listOfNotNull(player.benchmarkModel), "status" to listOf(player.matchStatus.name),
        "gentoolUpdatedAt" to listOfNotNull(player.gentoolUpdatedAt?.toString()),
        "scoreUpdatedAt" to listOfNotNull(player.scoreUpdatedAt?.toString()),
    )

    private fun enrich(rows: List<PeriodPlayer>): List<CpuPlayerDto> {
        if (rows.isEmpty()) return emptyList()
        val participants = replayPlayers.findByReplayIdIn(rows.map { it.replayId }).groupBy { it.replayId }
        return rows.map { row ->
            row.player.copy(latestMatch = CpuPlayerLatestMatchDto(
                matchAt = row.player.observedAt,
                teams = participants[row.replayId].orEmpty().sortedWith(compareBy({ it.teamNumber }, { it.slotNumber }))
                    .groupBy { it.teamNumber }.values.map { team -> team.map { it.name } },
            ))
        }
    }
}