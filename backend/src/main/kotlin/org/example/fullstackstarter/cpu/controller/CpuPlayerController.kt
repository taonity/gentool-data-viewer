package org.example.fullstackstarter.cpu.controller

import org.example.fullstackstarter.console.dto.PageResponse
import org.example.fullstackstarter.console.service.AccessGuard
import org.example.fullstackstarter.cpu.dto.CpuBenchmarkSyncDto
import org.example.fullstackstarter.cpu.dto.CpuPlayerDto
import org.example.fullstackstarter.cpu.dto.CpuPlayerSummaryDto
import org.example.fullstackstarter.cpu.service.CpuBenchmarkSyncService
import org.example.fullstackstarter.cpu.service.CpuPlayerQueryService
import org.example.fullstackstarter.security.principal.GoogleUserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/console/cpu-players")
class CpuPlayerController(
    private val queryService: CpuPlayerQueryService,
    private val syncService: CpuBenchmarkSyncService,
    private val accessGuard: AccessGuard,
) {
    @GetMapping
    fun list(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) field: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "score") sort: String,
        @RequestParam(defaultValue = "desc") direction: String,
    ): PageResponse<CpuPlayerDto> = queryService.list(q, field, page, size, sort, direction)

    @GetMapping("/summary")
    fun summary(): CpuPlayerSummaryDto = queryService.summary()

    @PostMapping("/benchmarks/refresh")
    fun refresh(@AuthenticationPrincipal principal: GoogleUserPrincipal): CpuBenchmarkSyncDto {
        accessGuard.requireAdmin(principal)
        return CpuBenchmarkSyncDto.from(syncService.sync())
    }
}