package org.taonity.gentooldataviewer.cpu.service

import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.console.dto.PageResponse
import org.taonity.gentooldataviewer.cpu.dto.CpuPlayerDto
import org.taonity.gentooldataviewer.cpu.dto.CpuPlayerSummaryDto
import org.taonity.gentooldataviewer.cpu.repository.CpuBenchmarkRepository
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class CpuPlayerQueryService(
    private val hardwareRepository: PlayerHardwareRepository,
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
        val sortProperty = cpuPlayerSortProperty(sort)
        val sortDirection = if (direction == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val order = Sort.Order(sortDirection, sortProperty).nullsLast()
        val pageable = PageRequest.of(
            page.coerceAtLeast(0),
            size.coerceIn(1, settings.console().maxPageSize),
            Sort.by(order, Sort.Order.asc("mainName"), Sort.Order.asc("playerId")),
        )
        val result = hardwareRepository.search(
            q = q?.trim().orEmpty(),
            field = field?.takeIf(String::isNotBlank) ?: "all",
            pageable = pageable,
        )
        return PageResponse.of(result) { CpuPlayerDto.from(it, objectMapper) }
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
        )
    }

}

internal val CPU_PLAYER_SORT_PROPERTIES = mapOf(
    "mainName" to "mainName",
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