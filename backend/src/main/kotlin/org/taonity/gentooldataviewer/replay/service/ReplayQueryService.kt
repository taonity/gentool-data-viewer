package org.taonity.gentooldataviewer.replay.service

import org.taonity.gentooldataviewer.common.config.AppProperties
import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.console.dto.PageResponse
import org.taonity.gentooldataviewer.replay.dto.ReplayDto
import org.taonity.gentooldataviewer.replay.dto.ReplayDateRangeDto
import org.taonity.gentooldataviewer.replay.dto.MatchDatePeriod
import java.time.LocalDate
import java.time.ZoneOffset
import org.taonity.gentooldataviewer.replay.repository.ReplayAssociatedFileRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayPlayerRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper

@Service
class ReplayQueryService(
    private val replayRepository: ReplayRepository,
    private val replayPlayerRepository: ReplayPlayerRepository,
    private val replayAssociatedFileRepository: ReplayAssociatedFileRepository,
    private val settings: AppSettings,
    private val appProperties: AppProperties,
    private val objectMapper: ObjectMapper,
) {
    @Transactional(readOnly = true)
    fun dateRange(): ReplayDateRangeDto {
        val bounds = replayRepository.findMatchDateBounds()
        val lastMatchDate = bounds.lastMatchAt?.atOffset(ZoneOffset.UTC)?.toLocalDate()
        if (lastMatchDate == null || lastMatchDate < appProperties.replayMinMatchDate) {
            return ReplayDateRangeDto(startDate = null, endDate = null)
        }
        return ReplayDateRangeDto(
            startDate = maxOf(
                bounds.firstMatchAt?.atOffset(ZoneOffset.UTC)?.toLocalDate() ?: appProperties.replayMinMatchDate,
                appProperties.replayMinMatchDate,
            ),
            endDate = lastMatchDate,
        )
    }

    @Transactional(readOnly = true)
    fun list(
        q: String?,
        field: String?,
        page: Int,
        size: Int,
        sort: String?,
        direction: String?,
        reporterIds: List<String> = emptyList(),
        replayId: String? = null,
        exact: Boolean = false,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
    ): PageResponse<ReplayDto> {
        val requestedPeriod = MatchDatePeriod(startDate, endDate)
        val minimumMatchAt = appProperties.replayMinMatchDate.atStartOfDay().toInstant(ZoneOffset.UTC)
        val fromDate = maxOf(requestedPeriod.from ?: minimumMatchAt, minimumMatchAt)
        val normalizedReporterIds = reporterIds.map(String::trim).filter(String::isNotEmpty).map(String::uppercase).distinct()
        val sortProperty = replaySortProperty(sort)
        val sortDirection = if (direction == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val order = Sort.Order(sortDirection, sortProperty).nullsLast()
        val pageable = PageRequest.of(
            page.coerceAtLeast(0),
            size.coerceIn(1, settings.console().maxPageSize),
            Sort.by(order, Sort.Order.asc("id")),
        )
        val result = replayRepository.search(
            q = q?.trim().orEmpty(),
            field = field?.takeIf(String::isNotBlank) ?: "all",
            pageable = pageable,
            reporterIds = normalizedReporterIds.ifEmpty { listOf("") },
            filterReporterIds = normalizedReporterIds.isNotEmpty(),
            replayId = replayId,
            exact = exact && !q.isNullOrBlank(),
            fromDate = fromDate,
            untilDate = requestedPeriod.until,
        )
        val replayIds = result.content.mapNotNull { it.id }
        val players = replayPlayerRepository.findByReplayIdIn(replayIds).groupBy { it.replayId }
        val files = replayAssociatedFileRepository.findByReplayIdIn(replayIds).groupBy { it.replayId }
        return PageResponse.of(result) { replay ->
            val replayId = requireNotNull(replay.id)
            ReplayDto.from(replay, players[replayId].orEmpty(), files[replayId].orEmpty(), objectMapper)
        }
    }

}

internal val REPLAY_SORT_PROPERTIES = mapOf(
    "matchAt" to "matchAt",
    "reporter" to "reporterName",
    "players" to "playerNames",
    "mapName" to "mapName",
    "matchType" to "matchType",
    "duration" to "matchLengthSeconds",
    "cpu" to "cpu",
    "matchMode" to "matchMode",
    "startCash" to "startCash",
    "gentoolVersion" to "gentoolVersion",
    "gameVersion" to "gameVersion",
    "windowsCompat" to "windowsCompat",
    "repInfoInUse" to "repInfoInUse",
    "replaySize" to "replaySizeBytes",
    "sourceDate" to "sourceDate",
    "collectedAt" to "collectedAt",
)

internal fun replaySortProperty(sort: String?): String = REPLAY_SORT_PROPERTIES[sort] ?: "matchAt"