package org.example.fullstackstarter.replay.controller

import org.example.fullstackstarter.console.service.AccessGuard
import org.example.fullstackstarter.replay.dto.ReplayCollectionJobDto
import org.example.fullstackstarter.replay.dto.StartReplayCollectionRequest
import org.example.fullstackstarter.replay.service.ReplayCollectionCoordinator
import org.example.fullstackstarter.replay.service.ReplayCollectionJobService
import org.example.fullstackstarter.security.principal.GoogleUserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/console/replay-collection")
class ReplayCollectionController(
    private val accessGuard: AccessGuard,
    private val coordinator: ReplayCollectionCoordinator,
    private val jobService: ReplayCollectionJobService,
) {
    @GetMapping("/jobs")
    fun jobs(@AuthenticationPrincipal principal: GoogleUserPrincipal): List<ReplayCollectionJobDto> {
        accessGuard.requireView(principal)
        return jobService.latest()
    }

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun start(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
        @RequestBody request: StartReplayCollectionRequest,
    ): Map<String, String> {
        val user = accessGuard.requireAdmin(principal)
        return mapOf(
            "jobId" to coordinator.startManual(request.startDate, request.endDate, user.email, request.userLimit),
        )
    }
}