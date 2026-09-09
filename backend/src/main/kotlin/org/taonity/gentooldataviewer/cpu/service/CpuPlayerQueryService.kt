package org.taonity.gentooldataviewer.cpu.service

import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.console.dto.PageResponse
import org.taonity.gentooldataviewer.cpu.dto.DiscordUserDto
import org.taonity.gentooldataviewer.cpu.dto.CpuPlayerDto
import org.taonity.gentooldataviewer.cpu.dto.CpuPlayerSummaryDto
import org.taonity.gentooldataviewer.cpu.repository.CpuBenchmarkRepository
import org.taonity.gentooldataviewer.replay.entity.GentoolLinkStatus
import org.taonity.gentooldataviewer.replay.repository.GentoolUserLinkRepository
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRepository
import org.taonity.gentooldataviewer.user.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class CpuPlayerQueryService(
    private val hardwareRepository: PlayerHardwareRepository,
    private val replayRepository: ReplayRepository,
    private val linkRepository: GentoolUserLinkRepository,
    private val userRepository: UserRepository,
    private val benchmarkRepository: CpuBenchmarkRepository,
    private val benchmarkSyncService: CpuBenchmarkSyncService,
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
    ): PageResponse<CpuPlayerDto> {
        val pageNumber = page.coerceAtLeast(0)
        val pageSize = size.coerceIn(1, settings.console().maxPageSize)
        val query = q?.trim().orEmpty()
        val searchField = field?.takeIf(String::isNotBlank) ?: "all"
        val sortProperty = cpuPlayerSortProperty(sort)
        val sortDirection = if (direction == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val order = Sort.Order(sortDirection, sortProperty).nullsLast()
        val result = if (sort == "discordUser") {
            hardwareRepository.searchSortedByDiscord(
                q = query,
                field = searchField,
                linkStatus = GentoolLinkStatus.APPROVED,
                ascending = sortDirection == Sort.Direction.ASC,
                pageable = PageRequest.of(pageNumber, pageSize),
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
            )
        }
        val linksByPlayerId = linkRepository.findByPlayerIdInAndStatus(
            result.content.map { it.playerId },
            GentoolLinkStatus.APPROVED,
        ).associateBy { it.playerId }
        val usersById = userRepository.findAllById(linksByPlayerId.values.map { it.userId })
            .associateBy { it.userId }
        return PageResponse.of(result) { player ->
            val user = linksByPlayerId[player.playerId]?.let { usersById[it.userId] }
            CpuPlayerDto.from(
                player,
                objectMapper,
                user?.let { DiscordUserDto(it.displayName, it.pictureUrl) },
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