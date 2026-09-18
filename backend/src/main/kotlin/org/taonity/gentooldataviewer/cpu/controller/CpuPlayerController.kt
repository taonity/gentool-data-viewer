package org.taonity.gentooldataviewer.cpu.controller

import org.taonity.gentooldataviewer.console.dto.PageResponse
import org.taonity.gentooldataviewer.console.service.AccessGuard
import org.taonity.gentooldataviewer.cpu.dto.CpuBenchmarkSyncDto
import org.taonity.gentooldataviewer.cpu.dto.CpuPlayerDto
import org.taonity.gentooldataviewer.cpu.dto.CpuPlayerSummaryDto
import org.taonity.gentooldataviewer.cpu.dto.RankedCpuPlayerDto
import org.taonity.gentooldataviewer.cpu.service.CpuBenchmarkSyncService
import org.taonity.gentooldataviewer.cpu.service.CpuPlayerQueryService
import org.taonity.gentooldataviewer.cpu.service.CpuPlayerPeriodService
import org.taonity.gentooldataviewer.replay.dto.MatchDatePeriod
import java.time.LocalDate
import org.taonity.gentooldataviewer.security.principal.GoogleUserPrincipal
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
    private val periodService: CpuPlayerPeriodService,
) {
    @GetMapping
    fun list(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) field: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "score") sort: String,
        @RequestParam(defaultValue = "desc") direction: String,
        @RequestParam(defaultValue = "false") linkedOnly: Boolean,
        @RequestParam(required = false) player: String?,
        @RequestParam(defaultValue = "false") exact: Boolean,
        @RequestParam(required = false) startDate: LocalDate?,
        @RequestParam(required = false) endDate: LocalDate?,
    ): PageResponse<CpuPlayerDto> {
        val period = MatchDatePeriod(startDate, endDate)
        return if (period.active) periodService.list(period, q, field, page, size, sort, direction, linkedOnly, player, exact)
        else queryService.list(q, field, page, size, sort, direction, linkedOnly, player, exact)
    }

    @GetMapping("/summary")
    fun summary(
        @RequestParam(required = false) startDate: LocalDate?,
        @RequestParam(required = false) endDate: LocalDate?,
    ): CpuPlayerSummaryDto {
        val period = MatchDatePeriod(startDate, endDate)
        return if (period.active) periodService.summary(period) else queryService.summary()
    }

    @GetMapping("/mine")
    fun mine(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
        @RequestParam(defaultValue = "replayCount") sort: String,
        @RequestParam(defaultValue = "desc") direction: String,
        @RequestParam(defaultValue = "false") linkedOnly: Boolean,
        @RequestParam(required = false) startDate: LocalDate?,
        @RequestParam(required = false) endDate: LocalDate?,
    ): List<RankedCpuPlayerDto> {
        val period = MatchDatePeriod(startDate, endDate)
        return if (period.active) periodService.linked(period, accessGuard.requireView(principal).userId, sort, direction, linkedOnly)
        else queryService.listLinked(principal, sort, direction, linkedOnly)
    }

    @PostMapping("/benchmarks/refresh")
    fun refresh(@AuthenticationPrincipal principal: GoogleUserPrincipal): CpuBenchmarkSyncDto {
        accessGuard.requireAdmin(principal)
        return CpuBenchmarkSyncDto.from(syncService.sync())
    }
}