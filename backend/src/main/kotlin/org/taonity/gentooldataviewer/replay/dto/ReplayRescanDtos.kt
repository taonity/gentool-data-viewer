package org.taonity.gentooldataviewer.replay.dto

import org.taonity.gentooldataviewer.replay.entity.CollectionStatus
import org.taonity.gentooldataviewer.replay.entity.GentoolLinkStatus
import java.time.Instant

data class GentoolLinkDto(
    val userId: String,
    val email: String,
    val displayName: String,
    val playerId: String,
    val playerName: String?,
    val status: GentoolLinkStatus,
    val requestedAt: Instant,
    val decidedAt: Instant?,
)

data class ReplayRescanHistoryDto(
    val id: String,
    val targetPlayerId: String,
    val ownTarget: Boolean,
    val jobId: String,
    val status: CollectionStatus,
    val errorMessage: String?,
    val requestedAt: Instant,
)

data class ReplayRescanDashboardDto(
    val link: GentoolLinkDto?,
    val otherUsedToday: Long,
    val otherDailyLimit: Long,
    val quotaResetsAt: Instant,
    val targetCooldownSeconds: Long,
    val lookbackDays: Long,
    val history: List<ReplayRescanHistoryDto>,
)

data class RequestGentoolLinkBody(val playerId: String)

data class RequestReplayRescanBody(val playerId: String)

data class ReplayRescanAcceptedDto(val jobId: String, val ownTarget: Boolean)