package org.taonity.gentooldataviewer.replay.service

import org.taonity.gentooldataviewer.console.entity.AuditAction
import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.console.exception.ConsoleNotFoundException
import org.taonity.gentooldataviewer.console.service.AccessGuard
import org.taonity.gentooldataviewer.console.service.AuditService
import org.taonity.gentooldataviewer.replay.config.ReplayRescanProperties
import org.taonity.gentooldataviewer.replay.dto.GentoolLinkDto
import org.taonity.gentooldataviewer.replay.dto.ReplayRescanAcceptedDto
import org.taonity.gentooldataviewer.replay.dto.ReplayRescanDashboardDto
import org.taonity.gentooldataviewer.replay.dto.ReplayRescanHistoryDto
import org.taonity.gentooldataviewer.replay.entity.GentoolLinkStatus
import org.taonity.gentooldataviewer.replay.entity.GentoolUserLinkEntity
import org.taonity.gentooldataviewer.replay.entity.ReplayRescanRequestEntity
import org.taonity.gentooldataviewer.replay.repository.GentoolUserLinkRepository
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayCollectionJobRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRescanRequestRepository
import org.taonity.gentooldataviewer.security.principal.GoogleUserPrincipal
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Service
class ReplayRescanService(
    private val properties: ReplayRescanProperties,
    private val settings: AppSettings,
    private val accessGuard: AccessGuard,
    private val linkRepository: GentoolUserLinkRepository,
    private val requestRepository: ReplayRescanRequestRepository,
    private val hardwareRepository: PlayerHardwareRepository,
    private val jobRepository: ReplayCollectionJobRepository,
    private val coordinator: ReplayCollectionCoordinator,
    private val auditService: AuditService,
) {
    private val clock = Clock.systemUTC()

    fun dashboard(principal: GoogleUserPrincipal): ReplayRescanDashboardDto {
        val user = accessGuard.currentUser(principal)
        val limits = limitsFor(user)
        val dayStart = LocalDate.now(clock).atStartOfDay().toInstant(ZoneOffset.UTC)
        return ReplayRescanDashboardDto(
            links = linkRepository.findByUserId(user.userId).map { toDto(it, user) }.sortedBy { it.playerName ?: it.playerId },
            maxLinkedPlayers = settings.replayRescan().maxLinkedPlayers,
            otherUsedToday = requestRepository
                .countByRequestedByUserIdAndOwnTargetFalseAndRequestedAtGreaterThanEqual(user.userId, dayStart),
            otherDailyLimit = limits.otherDailyLimit,
            quotaResetsAt = dayStart.plusSeconds(86_400),
            targetCooldownSeconds = limits.targetCooldown.seconds,
            lookbackDays = properties.lookbackDays,
            history = requestRepository.findTop10ByRequestedByUserIdOrderByRequestedAtDesc(user.userId).map { request ->
                val job = jobRepository.findById(request.jobId).orElseThrow()
                ReplayRescanHistoryDto(
                    id = requireNotNull(request.id),
                    targetPlayerId = request.targetPlayerId,
                    ownTarget = request.ownTarget,
                    jobId = request.jobId,
                    status = job.status,
                    errorMessage = job.errorMessage,
                    requestedAt = request.requestedAt,
                )
            },
        )
    }

    @Synchronized
    fun requestLink(principal: GoogleUserPrincipal, rawPlayerId: String): GentoolLinkDto {
        val user = accessGuard.currentUser(principal)
        val playerId = normalizePlayerId(rawPlayerId)
        if (!hardwareRepository.existsById(playerId)) throw ConsoleNotFoundException("GenTool player not found")
        val claimed = linkRepository.findByPlayerId(playerId)
        if (claimed != null && claimed.userId != user.userId) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "This GenTool player is already claimed by another account")
        }
        val existing = linkRepository.findByUserIdAndPlayerId(user.userId, playerId)
        if (existing?.status == GentoolLinkStatus.APPROVED) {
            return toDto(existing, user)
        }
        if (existing == null && linkRepository.countByUserId(user.userId) >= settings.replayRescan().maxLinkedPlayers) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "A Discord account can link at most ${settings.replayRescan().maxLinkedPlayers} GenTool players",
            )
        }
        val link = existing?.apply {
            status = GentoolLinkStatus.APPROVED
            requestedAt = Instant.now(clock)
            decidedAt = requestedAt
            decidedByUserId = user.userId
        } ?: GentoolUserLinkEntity(
            userId = user.userId,
            playerId = playerId,
            status = GentoolLinkStatus.APPROVED,
            decidedAt = Instant.now(clock),
            decidedByUserId = user.userId,
        )
        val saved = linkRepository.save(link)
        auditService.record(
            AuditAction.CLAIM_GENTOOL_LINK,
            "gentool_player",
            playerId,
            user,
        )
        return toDto(saved, user)
    }

    @Synchronized
    fun unlink(principal: GoogleUserPrincipal, rawPlayerId: String) {
        val user = accessGuard.currentUser(principal)
        val playerId = normalizePlayerId(rawPlayerId)
        val link = linkRepository.findByUserIdAndPlayerId(user.userId, playerId) ?: return
        linkRepository.delete(link)
        auditService.record(AuditAction.UNLINK_GENTOOL_LINK, "gentool_player", link.playerId, user)
    }

    @Synchronized
    fun requestRescan(principal: GoogleUserPrincipal, rawPlayerId: String): ReplayRescanAcceptedDto {
        val user = accessGuard.requireView(principal)
        val limits = limitsFor(user)
        val playerId = normalizePlayerId(rawPlayerId)
        if (!hardwareRepository.existsById(playerId)) throw ConsoleNotFoundException("GenTool player not found")
        val ownPlayerIds = linkRepository.findByUserId(user.userId)
            .filter { it.status == GentoolLinkStatus.APPROVED }
            .mapTo(mutableSetOf()) { it.playerId }
        val ownTarget = playerId in ownPlayerIds
        val now = Instant.now(clock)
        val latest = requestRepository
            .findTopByRequestedByUserIdAndTargetPlayerIdOrderByRequestedAtDesc(user.userId, playerId)
        if (latest != null && latest.requestedAt.plus(limits.targetCooldown).isAfter(now)) {
            throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "This player was rescanned recently")
        }
        if (!ownTarget) {
            val dayStart = LocalDate.now(clock).atStartOfDay().toInstant(ZoneOffset.UTC)
            val used = requestRepository
                .countByRequestedByUserIdAndOwnTargetFalseAndRequestedAtGreaterThanEqual(user.userId, dayStart)
            if (used >= limits.otherDailyLimit) {
                throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Daily rescan quota reached")
            }
        }
        val endDate = LocalDate.now(clock)
        val startDate = endDate.minusDays(properties.lookbackDays - 1)
        val jobId = coordinator.startUserRescan(startDate, endDate, user.userId, playerId)
        requestRepository.save(
            ReplayRescanRequestEntity(
                requestedByUserId = user.userId,
                targetPlayerId = playerId,
                ownTarget = ownTarget,
                jobId = jobId,
                requestedAt = now,
            ),
        )
        auditService.record(AuditAction.REQUEST_RESCAN, "gentool_player", playerId, user)
        return ReplayRescanAcceptedDto(jobId, ownTarget)
    }

    private fun normalizePlayerId(value: String): String {
        val playerId = value.trim().uppercase()
        if (!PLAYER_ID.matches(playerId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "GenTool player ID must contain 12 hexadecimal characters")
        }
        return playerId
    }

    private fun limitsFor(user: UserEntity): RescanLimits =
        if (user.role == ConsoleRole.OWNER || user.role == ConsoleRole.ADMIN) {
            RescanLimits(properties.privilegedOtherDailyLimit, properties.privilegedTargetCooldown)
        } else {
            RescanLimits(properties.otherDailyLimit, properties.targetCooldown)
        }

    private fun toDto(link: GentoolUserLinkEntity, user: UserEntity) = GentoolLinkDto(
        userId = link.userId,
        discordUserId = user.userId.substringAfter("discord:"),
        displayName = user.displayName,
        playerId = link.playerId,
        playerName = hardwareRepository.findById(link.playerId).orElse(null)?.mainName,
        status = link.status,
        requestedAt = link.requestedAt,
        decidedAt = link.decidedAt,
    )

    private companion object {
        val PLAYER_ID = Regex("[0-9A-F]{12}")
    }

    private data class RescanLimits(
        val otherDailyLimit: Long,
        val targetCooldown: java.time.Duration,
    )
}