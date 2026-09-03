package org.taonity.gentooldataviewer.console.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.taonity.gentooldataviewer.console.entity.AuditAction
import org.taonity.gentooldataviewer.console.entity.AuditLogEntity
import org.taonity.gentooldataviewer.console.repository.AuditLogRepository
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository,
) {
    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    @Transactional
    fun record(action: AuditAction, targetType: String, targetId: String?, actor: UserEntity) {
        auditLogRepository.save(
            AuditLogEntity(
                action = action,
                targetType = targetType,
                targetId = targetId,
                actorGoogleId = actor.googleId,
                actorEmail = actor.email,
            )
        )
        LOGGER.info { "Audit: action=$action targetType=$targetType targetId=$targetId actor=${actor.googleId}" }
    }
}
