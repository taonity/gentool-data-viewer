package org.taonity.gentooldataviewer.user.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.taonity.gentooldataviewer.common.config.ConsoleProperties
import org.taonity.gentooldataviewer.security.principal.GoogleUserPrincipal
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.repository.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val consoleProperties: ConsoleProperties,
) {
    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    @Transactional
    fun createOrUpdateUser(principal: GoogleUserPrincipal) {
        val userId = principal.getUserId()
        val isOwner = consoleProperties.isOwner(userId)
        val isAdmin = consoleProperties.isAdmin(userId)
        val existing = userRepository.findById(userId).orElse(null)
        if (existing != null) {
            existing.updateDetails(principal.getDisplayName(), principal.getEmail(), principal.getPictureUrl())
            if (isOwner && existing.role != ConsoleRole.OWNER) {
                existing.grantOwner()
                LOGGER.info { "Granted owner console access to bootstrapped user: ${existing.googleId}" }
            } else if (isAdmin && !existing.role.isAdmin()) {
                existing.grantAdmin()
                LOGGER.info { "Granted admin console access to bootstrapped user: ${existing.googleId}" }
            }
            LOGGER.debug { "Updated user: ${existing.googleId}" }
        } else {
            val legacyUser = userRepository.findFirstByEmailIgnoreCase(principal.getEmail())
                ?.takeIf { it.authProvider == "google" }
            val newUser = UserEntity(
                googleId = principal.getUserId(),
                authProvider = principal.userInfo.provider,
                email = principal.getEmail(),
                displayName = principal.getDisplayName(),
                pictureUrl = principal.getPictureUrl(),
                role = legacyUser?.role ?: ConsoleRole.VIEWER,
                accessStatus = legacyUser?.accessStatus
                    ?: org.taonity.gentooldataviewer.user.entity.AccessRequestStatus.APPROVED,
                requestedRole = legacyUser?.requestedRole,
            )
            if (isOwner) {
                newUser.grantOwner()
                LOGGER.info { "Bootstrapped owner console user: ${newUser.googleId}" }
            } else if (isAdmin) {
                newUser.grantAdmin()
                LOGGER.info { "Bootstrapped admin console user: ${newUser.googleId}" }
            }
            userRepository.save(newUser)
            if (legacyUser != null) {
                userRepository.delete(legacyUser)
                LOGGER.info { "Migrated legacy Google access to Discord user: ${newUser.googleId}" }
            }
            LOGGER.info { "Created new user: ${newUser.googleId}" }
        }
    }
}
