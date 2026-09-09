package org.taonity.gentooldataviewer.replay.controller

import org.taonity.gentooldataviewer.replay.dto.GentoolLinkDto
import org.taonity.gentooldataviewer.replay.dto.ReplayRescanAcceptedDto
import org.taonity.gentooldataviewer.replay.dto.ReplayRescanDashboardDto
import org.taonity.gentooldataviewer.replay.dto.RequestGentoolLinkBody
import org.taonity.gentooldataviewer.replay.dto.RequestReplayRescanBody
import org.taonity.gentooldataviewer.replay.service.ReplayRescanService
import org.taonity.gentooldataviewer.security.principal.GoogleUserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/console/replay-rescans")
class ReplayRescanController(
    private val service: ReplayRescanService,
) {
    @GetMapping("/me")
    fun dashboard(@AuthenticationPrincipal principal: GoogleUserPrincipal): ReplayRescanDashboardDto =
        service.dashboard(principal)

    @PostMapping("/link")
    fun requestLink(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
        @RequestBody body: RequestGentoolLinkBody,
    ): GentoolLinkDto = service.requestLink(principal, body.playerId)

    @DeleteMapping("/link/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unlink(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
        @PathVariable playerId: String,
    ) = service.unlink(principal, playerId)

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    fun requestRescan(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
        @RequestBody body: RequestReplayRescanBody,
    ): ReplayRescanAcceptedDto = service.requestRescan(principal, body.playerId)

}