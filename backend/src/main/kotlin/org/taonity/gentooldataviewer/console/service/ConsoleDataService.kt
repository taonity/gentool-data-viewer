package org.taonity.gentooldataviewer.console.service

import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.console.dto.AuditLogDto
import org.taonity.gentooldataviewer.console.dto.PageResponse
import org.taonity.gentooldataviewer.console.repository.AuditLogRepository
import org.taonity.gentooldataviewer.security.principal.GoogleUserPrincipal
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service

@Service
class ConsoleDataService(
    private val auditLogRepository: AuditLogRepository,
    private val accessGuard: AccessGuard,
    private val settings: AppSettings,
) {
    fun listAuditLogs(principal: GoogleUserPrincipal, q: String?, field: String?, page: Int, size: Int): PageResponse<AuditLogDto> {
        accessGuard.requireAdmin(principal)
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, settings.console().maxPageSize))
        val result = if (q.isNullOrBlank()) {
            auditLogRepository.findAllByOrderByOccurredAtDesc(pageable)
        } else {
            auditLogRepository.search(q.trim(), field.orAllField(), pageable)
        }
        return PageResponse.of(result, AuditLogDto::from)
    }

    private fun String?.orAllField(): String = this?.takeIf { it.isNotBlank() } ?: "all"
}
