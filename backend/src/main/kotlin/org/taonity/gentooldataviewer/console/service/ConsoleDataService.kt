package org.taonity.gentooldataviewer.console.service

import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.console.dto.AuditLogDto
import org.taonity.gentooldataviewer.console.dto.AuditLogDateRangeDto
import org.taonity.gentooldataviewer.console.dto.PageResponse
import org.taonity.gentooldataviewer.console.repository.AuditLogRepository
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserPrincipal
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneOffset
import org.taonity.gentooldataviewer.console.exception.ConsoleValidationException

@Service
class ConsoleDataService(
    private val auditLogRepository: AuditLogRepository,
    private val accessGuard: AccessGuard,
    private val settings: AppSettings,
) {
    fun auditLogDateRange(principal: AuthenticatedUserPrincipal): AuditLogDateRangeDto {
        accessGuard.requireAdmin(principal)
        val range = auditLogRepository.dateRange()
        return AuditLogDateRangeDto(
            range.earliest?.atZone(ZoneOffset.UTC)?.toLocalDate(),
            range.latest?.atZone(ZoneOffset.UTC)?.toLocalDate(),
        )
    }

    fun listAuditLogs(
        principal: AuthenticatedUserPrincipal,
        q: String?,
        field: String?,
        sort: String,
        direction: String,
        page: Int,
        size: Int,
        exact: Boolean = false,
        startDate: LocalDate? = null,
        endDate: LocalDate? = null,
    ): PageResponse<AuditLogDto> {
        accessGuard.requireAdmin(principal)
        if (startDate != null && endDate != null && startDate > endDate) {
            throw ConsoleValidationException("Start date must not be after end date.")
        }
        if (listOfNotNull(startDate, endDate).any { it.year !in 1..9999 }) {
            throw ConsoleValidationException("Dates must use years between 0001 and 9999.")
        }
        val sortProperty = AUDIT_SORT_FIELDS[sort] ?: "occurredAt"
        val sortDirection = if (direction.equals("asc", ignoreCase = true)) Sort.Direction.ASC else Sort.Direction.DESC
        val pageable = PageRequest.of(
            page.coerceAtLeast(0),
            size.coerceIn(1, settings.console().maxPageSize),
            Sort.by(sortDirection, sortProperty).and(Sort.by(Sort.Direction.DESC, "id")),
        )
        val result = if (q.isNullOrBlank() && startDate == null && endDate == null) {
            auditLogRepository.findAll(pageable)
        } else {
            auditLogRepository.search(
                q?.trim().orEmpty(), field.orAllField(), exact,
                startDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant(),
                endDate?.plusDays(1)?.atStartOfDay(ZoneOffset.UTC)?.toInstant(),
                pageable,
            )
        }
        return PageResponse.of(result, AuditLogDto::from)
    }

    private fun String?.orAllField(): String = this?.takeIf { it.isNotBlank() } ?: "all"

    companion object {
        private val AUDIT_SORT_FIELDS = mapOf(
            "occurredAt" to "occurredAt",
            "action" to "action",
            "targetType" to "targetType",
            "targetId" to "targetId",
            "actorUserId" to "actorUserId",
        )
    }
}
