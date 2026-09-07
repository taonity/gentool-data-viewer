package org.taonity.gentooldataviewer.console.dto

import org.taonity.gentooldataviewer.console.entity.AuditLogEntity
import java.time.Instant

data class AuditLogDto(
    val id: String?,
    val action: String,
    val targetType: String,
    val targetId: String?,
    val actorUserId: String,
    val occurredAt: Instant,
) {
    companion object {
        fun from(e: AuditLogEntity) = AuditLogDto(
            id = e.id,
            action = e.action.name,
            targetType = e.targetType,
            targetId = e.targetId,
            actorUserId = e.actorUserId,
            occurredAt = e.occurredAt,
        )
    }
}

data class PageLocation(val page: Int)
