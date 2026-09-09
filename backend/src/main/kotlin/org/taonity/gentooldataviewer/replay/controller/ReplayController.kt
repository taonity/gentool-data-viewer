package org.taonity.gentooldataviewer.replay.controller

import org.taonity.gentooldataviewer.console.dto.PageResponse
import org.taonity.gentooldataviewer.replay.dto.ReplayDto
import org.taonity.gentooldataviewer.replay.service.ReplayQueryService
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
        @RequestParam(required = false) reporterId: String?,
        @RequestParam(required = false) replay: String?,
    ): PageResponse<ReplayDto> = replayQueryService.list(q, field, page, size, sort, direction, reporterId, replay)
}