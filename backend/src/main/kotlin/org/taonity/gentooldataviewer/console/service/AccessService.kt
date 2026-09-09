package org.taonity.gentooldataviewer.console.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.console.dto.AccessInfoResponse
import org.taonity.gentooldataviewer.console.dto.PendingRequestDto
import org.taonity.gentooldataviewer.console.dto.UserSummaryDto
import org.taonity.gentooldataviewer.console.entity.AuditAction
import org.taonity.gentooldataviewer.console.exception.ConsoleForbiddenException
import org.taonity.gentooldataviewer.console.exception.ConsoleNotFoundException
import org.taonity.gentooldataviewer.security.principal.GoogleUserPrincipal
import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccessService(
    private val userRepository: UserRepository,
    private val accessGuard: AccessGuard,
    private val auditService: AuditService,
    private val settings: AppSettings,
) {
    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    fun describe(principal: GoogleUserPrincipal): AccessInfoResponse {
        val user = accessGuard.currentUser(principal)
        return AccessInfoResponse(
            userId = user.userId,
            displayName = user.displayName,
            role = user.role,
            accessStatus = user.accessStatus,
            requestedRole = user.requestedRole,
            canView = user.role.canView(),
            canEdit = user.role.canEdit(),
            isAdmin = user.role.isAdmin(),
            isOwner = user.role.isOwner(),
            accessRequestsEnabled = settings.console().accessRequestsEnabled,
        )
    }

    @Transactional
    fun requestAccess(principal: GoogleUserPrincipal, requestedRole: ConsoleRole): AccessInfoResponse {
        if (!settings.console().accessRequestsEnabled) {
            throw ConsoleForbiddenException("Access requests are disabled")
        }
        if (requestedRole != ConsoleRole.VIEWER && requestedRole != ConsoleRole.EDITOR) {
            throw ConsoleForbiddenException("Only VIEWER or EDITOR access can be requested")
        }
        val user = accessGuard.currentUser(principal)
        if (user.role.isAdmin()) {
            throw ConsoleForbiddenException("Admins already have full access")
        }
        if (user.role.rank() >= requestedRole.rank()) {
            throw ConsoleForbiddenException("You already have at least this level of access")
        }
        user.accessStatus = AccessRequestStatus.PENDING
        user.requestedRole = requestedRole
        auditService.record(AuditAction.REQUEST_ACCESS, "access_request", user.googleId, user)
        LOGGER.info { "User ${user.googleId} requested $requestedRole access" }
        return describe(principal)
    }

    fun listPendingRequests(principal: GoogleUserPrincipal): List<PendingRequestDto> {
        accessGuard.requireAdmin(principal)
        return userRepository.findByAccessStatusOrderByDisplayNameAsc(AccessRequestStatus.PENDING)
            .map { PendingRequestDto(it.googleId, it.userId.substringAfter("discord:"), it.displayName, it.requestedRole) }
    }

    @Transactional
    fun approve(principal: GoogleUserPrincipal, targetGoogleId: String, grantedRole: ConsoleRole) {
        val admin = accessGuard.requireAdmin(principal)
        if (grantedRole != ConsoleRole.VIEWER && grantedRole != ConsoleRole.EDITOR) {
            throw ConsoleForbiddenException("Granted role must be VIEWER or EDITOR")
        }
        val target = userRepository.findById(targetGoogleId)
            .orElseThrow { ConsoleNotFoundException("User not found") }
        target.role = grantedRole
        target.accessStatus = AccessRequestStatus.APPROVED
        target.requestedRole = null
        auditService.record(AuditAction.APPROVE_ACCESS, "access_request", target.googleId, admin)
        LOGGER.info { "Admin ${admin.googleId} approved $grantedRole for ${target.googleId}" }
    }

    @Transactional
    fun reject(principal: GoogleUserPrincipal, targetGoogleId: String) {
        val admin = accessGuard.requireAdmin(principal)
        val target = userRepository.findById(targetGoogleId)
            .orElseThrow { ConsoleNotFoundException("User not found") }
        target.role = ConsoleRole.NONE
        target.accessStatus = AccessRequestStatus.REJECTED
        target.requestedRole = null
        auditService.record(AuditAction.REJECT_ACCESS, "access_request", target.googleId, admin)
        LOGGER.info { "Admin ${admin.googleId} rejected access for ${target.googleId}" }
    }

    fun listUsers(principal: GoogleUserPrincipal): List<UserSummaryDto> {
        accessGuard.requireAdmin(principal)
        return userRepository.findAllByOrderByDisplayNameAsc().map {
            UserSummaryDto(
                it.googleId,
                it.userId.substringAfter("discord:"),
                it.displayName,
                it.role,
                it.accessStatus,
                it.requestedRole,
            )
        }
    }

    @Transactional
    fun changeRole(
        principal: GoogleUserPrincipal,
        targetGoogleId: String,
        newRole: ConsoleRole,
    ): UserSummaryDto {
        val actor = accessGuard.requireAdmin(principal)
        val target = userRepository.findById(targetGoogleId)
            .orElseThrow { ConsoleNotFoundException("User not found") }
        if (target.googleId == actor.googleId) {
            throw ConsoleForbiddenException("You cannot change your own role")
        }

        if (newRole.isAdmin() || target.role.isAdmin()) {
            if (!actor.role.isOwner()) {
                throw ConsoleForbiddenException("Only the owner can manage admin roles")
            }
        }
        target.role = newRole
        target.accessStatus =
            if (newRole == ConsoleRole.NONE) AccessRequestStatus.NONE else AccessRequestStatus.APPROVED
        target.requestedRole = null
        auditService.record(AuditAction.CHANGE_ROLE, "user", target.googleId, actor)
        LOGGER.info { "User ${actor.googleId} set role of ${target.googleId} to $newRole" }
        return UserSummaryDto(
            target.googleId,
            target.userId.substringAfter("discord:"),
            target.displayName,
            target.role,
            target.accessStatus,
            target.requestedRole,
        )
    }
}
