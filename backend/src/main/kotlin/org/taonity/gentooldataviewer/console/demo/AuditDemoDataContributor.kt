package org.taonity.gentooldataviewer.console.demo

import org.taonity.gentooldataviewer.common.demo.DemoDataContributor
import org.taonity.gentooldataviewer.console.entity.AuditAction
import org.taonity.gentooldataviewer.console.entity.AuditLogEntity
import org.taonity.gentooldataviewer.console.repository.AuditLogRepository
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

@Component
@Profile("demo-data")
@Order(200)
class AuditDemoDataContributor(
    private val auditLogRepository: AuditLogRepository,
) : DemoDataContributor {
    override val feature: String = "audit-log"

    @Transactional
    override fun seed(): Int {
        if (auditLogRepository.existsByActorGoogleId(DEMO_ACTOR_ID)) return 0
        val records = fixtures()
        auditLogRepository.saveAll(records)
        return records.size
    }

    private fun fixtures(): List<AuditLogEntity> {
        val now = Instant.now()
        return listOf(
            record(AuditAction.REQUEST_ACCESS, "access_request", "google-user-alice", now.minus(55, ChronoUnit.MINUTES)),
            record(AuditAction.REQUEST_ACCESS, "access_request", "google-user-bob", now.minus(42, ChronoUnit.MINUTES)),
            record(AuditAction.APPROVE_ACCESS, "access_request", "demo-approved-user", now.minus(28, ChronoUnit.MINUTES)),
            record(AuditAction.CHANGE_ROLE, "user", "demo-editor-user", now.minus(16, ChronoUnit.MINUTES)),
            record(AuditAction.EDIT_CONFIG, "config_override", "app.console.max-page-size", now.minus(6, ChronoUnit.MINUTES)),
        )
    }

    private fun record(
        action: AuditAction,
        targetType: String,
        targetId: String,
        occurredAt: Instant,
    ) = AuditLogEntity(
        action = action,
        targetType = targetType,
        targetId = targetId,
        actorGoogleId = DEMO_ACTOR_ID,
        actorEmail = "owner.demo@example.com",
        occurredAt = occurredAt,
    )

    private companion object {
        const val DEMO_ACTOR_ID = "demo-data-owner"
    }
}
