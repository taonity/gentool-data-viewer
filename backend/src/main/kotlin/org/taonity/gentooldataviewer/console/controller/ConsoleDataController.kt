package org.taonity.gentooldataviewer.console.controller

import org.taonity.gentooldataviewer.console.dto.AuditLogDto
import org.taonity.gentooldataviewer.console.dto.PageResponse
import org.taonity.gentooldataviewer.console.service.ConsoleDataService
import org.taonity.gentooldataviewer.security.principal.GoogleUserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/console")
class ConsoleDataController(
    private val consoleDataService: ConsoleDataService,
) {
    @GetMapping("/audit-logs")
    fun auditLogs(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) field: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
    ): PageResponse<AuditLogDto> = consoleDataService.listAuditLogs(principal, q, field, page, size)
}
