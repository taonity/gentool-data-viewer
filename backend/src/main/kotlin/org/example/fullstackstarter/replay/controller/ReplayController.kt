package org.example.fullstackstarter.replay.controller

import org.example.fullstackstarter.console.dto.PageResponse
import org.example.fullstackstarter.replay.dto.ReplayDto
import org.example.fullstackstarter.replay.service.ReplayQueryService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/console/replays")
class ReplayController(
    private val replayQueryService: ReplayQueryService,
) {
    @GetMapping
    fun list(
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) field: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "50") size: Int,
        @RequestParam(defaultValue = "matchAt") sort: String,
        @RequestParam(defaultValue = "desc") direction: String,
    ): PageResponse<ReplayDto> = replayQueryService.list(q, field, page, size, sort, direction)
}