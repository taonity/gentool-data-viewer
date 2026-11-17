package org.taonity.gentooldataviewer.console.repository

import org.taonity.gentooldataviewer.console.entity.AuditLogEntity
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface AuditLogRepository : JpaRepository<AuditLogEntity, String> {
    fun existsByActorUserId(actorUserId: String): Boolean

    @Query(
        """
        SELECT a FROM AuditLogEntity a
        WHERE (:startTime IS NULL OR a.occurredAt >= :startTime)
          AND (:endTime IS NULL OR a.occurredAt < :endTime)
          AND (:q = '' OR
            ((:field = 'all' OR :field = 'action') AND ((:exact = true AND LOWER(string(a.action)) = LOWER(:q)) OR (:exact = false AND LOWER(string(a.action)) LIKE LOWER(CONCAT('%', :q, '%')))))
            OR ((:field = 'all' OR :field = 'targetType') AND ((:exact = true AND LOWER(a.targetType) = LOWER(:q)) OR (:exact = false AND LOWER(a.targetType) LIKE LOWER(CONCAT('%', :q, '%')))))
            OR ((:field = 'all' OR :field = 'targetId') AND ((:exact = true AND LOWER(COALESCE(a.targetId, '')) = LOWER(:q)) OR (:exact = false AND LOWER(COALESCE(a.targetId, '')) LIKE LOWER(CONCAT('%', :q, '%')))))
            OR ((:field = 'all' OR :field = 'actorUserId') AND ((:exact = true AND LOWER(a.actorUserId) = LOWER(:q)) OR (:exact = false AND LOWER(a.actorUserId) LIKE LOWER(CONCAT('%', :q, '%'))))))
        """,
    )
    fun search(q: String, field: String, exact: Boolean, startTime: Instant?, endTime: Instant?, pageable: Pageable): Page<AuditLogEntity>

    interface DateRange {
        val earliest: Instant?
        val latest: Instant?
    }

    @Query("SELECT MIN(a.occurredAt) AS earliest, MAX(a.occurredAt) AS latest FROM AuditLogEntity a")
    fun dateRange(): DateRange

    @Modifying
    fun deleteByOccurredAtBefore(cutoff: Instant): Int
}
