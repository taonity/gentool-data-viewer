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

    fun findAllByOrderByOccurredAtDesc(pageable: Pageable): Page<AuditLogEntity>

    @Query(
        """
        SELECT a FROM AuditLogEntity a
          WHERE ((:field = 'all' OR :field = 'action') AND LOWER(string(a.action)) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
              OR ((:field = 'all' OR :field = 'targetType') AND LOWER(a.targetType) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
              OR ((:field = 'all' OR :field = 'targetId') AND LOWER(COALESCE(a.targetId, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
              OR ((:field = 'all' OR :field = 'actorUserId') AND LOWER(a.actorUserId) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
        ORDER BY a.occurredAt DESC
        """,
    )
    fun search(q: String, field: String, pageable: Pageable, exact: Boolean = false): Page<AuditLogEntity>

    @Modifying
    fun deleteByOccurredAtBefore(cutoff: Instant): Int
}
