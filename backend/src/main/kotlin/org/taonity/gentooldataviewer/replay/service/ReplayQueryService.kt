package org.taonity.gentooldataviewer.replay.service

import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.console.dto.PageResponse
import org.taonity.gentooldataviewer.replay.dto.ReplayDto
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
    ): PageResponse<ReplayDto> {
        val sortProperty = replaySortProperty(sort)
        val sortDirection = if (direction == "asc") Sort.Direction.ASC else Sort.Direction.DESC
        val order = Sort.Order(sortDirection, sortProperty).nullsLast()
        val pageable = PageRequest.of(
            page.coerceAtLeast(0),
            size.coerceIn(1, settings.console().maxPageSize),
            Sort.by(order, Sort.Order.asc("id")),
        )
        val result = if (q.isNullOrBlank()) {
            replayRepository.findAll(pageable)
        } else {
            replayRepository.search(q.trim(), field?.takeIf(String::isNotBlank) ?: "all", pageable)
        }
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