package org.taonity.gentooldataviewer.cpu.service

import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.console.dto.PageResponse
import org.taonity.gentooldataviewer.cpu.dto.DiscordUserDto
import org.taonity.gentooldataviewer.cpu.dto.CpuPlayerDto
import org.taonity.gentooldataviewer.cpu.dto.CpuPlayerLatestMatchDto
import org.taonity.gentooldataviewer.cpu.dto.CpuPlayerSummaryDto
import org.taonity.gentooldataviewer.cpu.dto.RankedCpuPlayerDto
import org.taonity.gentooldataviewer.cpu.repository.CpuBenchmarkRepository
import org.taonity.gentooldataviewer.cpu.repository.CpuPlayerRankingRepository
import org.taonity.gentooldataviewer.replay.entity.GentoolLinkStatus
import org.taonity.gentooldataviewer.replay.entity.PlayerHardwareEntity
import org.taonity.gentooldataviewer.replay.repository.GentoolUserLinkRepository
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayPlayerRepository
import org.taonity.gentooldataviewer.user.repository.UserRepository
import org.taonity.gentooldataviewer.console.service.AccessGuard
import org.taonity.gentooldataviewer.security.principal.GoogleUserPrincipal
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class CpuPlayerQueryService(
    private val hardwareRepository: PlayerHardwareRepository,
    private val replayRepository: ReplayRepository,
    private val replayPlayerRepository: ReplayPlayerRepository,
    private val linkRepository: GentoolUserLinkRepository,
    private val userRepository: UserRepository,
    private val benchmarkRepository: CpuBenchmarkRepository,
    private val benchmarkSyncService: CpuBenchmarkSyncService,
    private val rankingRepository: CpuPlayerRankingRepository,
    private val accessGuard: AccessGuard,
    private val settings: AppSettings,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
    fun list(
        q: String?,
        field: String?,
        page: Int,
        size: Int,
        sort: String?,
        direction: String?,
        linkedOnly: Boolean = false,
        playerId: String? = null,
        exact: Boolean = false,
    ): PageResponse<CpuPlayerDto> {
        val pageNumber = page.coerceAtLeast(0)
        val pageSize = size.coerceIn(1, settings.console().maxPageSize)
        val query = q?.trim().orEmpty()
        val searchField = field?.takeIf(String::isNotBlank) ?: "all"
        val sortProperty = cpuPlayerSortProperty(sort)
        val sortDirection = if (direction == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val targetPlayerId = playerId?.trim()?.uppercase()?.takeIf(String::isNotEmpty)
        val order = Sort.Order(sortDirection, sortProperty).nullsLast()
        val result = if (sort == "discordUser") {
            hardwareRepository.searchSortedByDiscord(
                q = query,
                field = searchField,
                linkStatus = GentoolLinkStatus.APPROVED,
                ascending = sortDirection == Sort.Direction.ASC,
                pageable = PageRequest.of(pageNumber, pageSize),
                linkedOnly = linkedOnly,
                playerId = targetPlayerId,
                exact = exact,
            )
        } else {
            hardwareRepository.search(
                q = query,
                field = searchField,
                pageable = PageRequest.of(
                    pageNumber,
                    pageSize,
                    Sort.by(order, Sort.Order.asc("mainName"), Sort.Order.asc("playerId")),
                ),
                linkedOnly = linkedOnly,
                linkStatus = GentoolLinkStatus.APPROVED,
                playerId = targetPlayerId,
                exact = exact,
            )
        }
        val playersById = playerDtos(result.content)
        return PageResponse.of(result) { player -> playersById.getValue(player.playerId) }
    }

    @Transactional(readOnly = true)
    fun listLinked(
        principal: GoogleUserPrincipal,
        sort: String?,
        direction: String?,
        linkedOnly: Boolean,
    ): List<RankedCpuPlayerDto> {
        val user = accessGuard.requireView(principal)
        val playerIds = linkRepository.findByUserId(user.userId)
            .filter { it.status == GentoolLinkStatus.APPROVED }
            .map { it.playerId }
        if (playerIds.isEmpty()) return emptyList()
        val players = hardwareRepository.findAllById(playerIds)
        val playersById = playerDtos(players)
        val ranks = rankingRepository.findRanks(playerIds, sort, direction, linkedOnly)
        return playerIds.mapNotNull { playerId ->
            val player = playersById[playerId] ?: return@mapNotNull null
            val rank = ranks[playerId] ?: return@mapNotNull null
            RankedCpuPlayerDto(rank, player)
        }.sortedBy { it.rank }
    }

    private fun playerDtos(players: List<PlayerHardwareEntity>): Map<String, CpuPlayerDto> {
        val linksByPlayerId = linkRepository.findByPlayerIdInAndStatus(
            players.map { it.playerId },
            GentoolLinkStatus.APPROVED,
        ).associateBy { it.playerId }
        val usersById = userRepository.findAllById(linksByPlayerId.values.map { it.userId })
            .associateBy { it.userId }
        val latestReplaysByPlayerId = replayRepository.findLatestByReporterIds(players.map { it.playerId })
            .associateBy { it.reporterId }
        val latestReplayPlayers = replayPlayerRepository.findByReplayIdIn(
            latestReplaysByPlayerId.values.mapNotNull { it.id },
        ).groupBy { it.replayId }
        return players.associate { player ->
            val user = linksByPlayerId[player.playerId]?.let { usersById[it.userId] }
            val latestReplay = latestReplaysByPlayerId[player.playerId]
            val latestMatch = latestReplay?.let { replay ->
                CpuPlayerLatestMatchDto(
                    matchAt = replay.matchAt,
                    teams = latestReplayPlayers[replay.id].orEmpty()
                        .sortedWith(compareBy({ it.teamNumber }, { it.slotNumber }))
                        .groupBy { it.teamNumber }
                        .values
                        .map { team -> team.map { it.name } },
                )
            }
            player.playerId to CpuPlayerDto.from(
                player,
                objectMapper,
                user?.let { DiscordUserDto(it.displayName, it.pictureUrl) },
                latestMatch,
            )
        }
    }

    @Transactional(readOnly = true)
    fun summary(): CpuPlayerSummaryDto {
        return CpuPlayerSummaryDto(
            totalPlayers = hardwareRepository.count(),
            ratedPlayers = hardwareRepository.countByCpuScoreIsNotNull(),
            unmatchedPlayers = hardwareRepository.countByCpuMatchStatus(CpuMatchStatus.UNMATCHED),
            ambiguousPlayers = hardwareRepository.countByCpuMatchStatus(CpuMatchStatus.AMBIGUOUS),
            playersWithoutCpu = hardwareRepository.countByCpuMatchStatus(CpuMatchStatus.NO_CPU),
            catalogEntries = benchmarkRepository.count(),
            catalogFetchedAt = benchmarkSyncService.latestFetchedAt(),
            dataSince = replayRepository.findEarliestCollectedAt(),
        )
    }

}

internal val CPU_PLAYER_SORT_PROPERTIES = mapOf(
    "mainName" to "mainName",
    "discordUser" to "discordUser",
    "playerId" to "playerId",
    "aliases" to "aliasesJson",
    "replayCount" to "replayCount",
    "reportedCpu" to "cpu",
    "score" to "cpuScore",
    "latestName" to "latestName",
    "benchmark" to "cpuBenchmarkName",
    "status" to "cpuMatchStatus",
    "observedAt" to "observedAt",
    "gentoolUpdatedAt" to "gentoolUpdatedAt",
    "scoreUpdatedAt" to "cpuScoreUpdatedAt",
)

internal fun cpuPlayerSortProperty(sort: String?): String = CPU_PLAYER_SORT_PROPERTIES[sort] ?: "cpuScore"