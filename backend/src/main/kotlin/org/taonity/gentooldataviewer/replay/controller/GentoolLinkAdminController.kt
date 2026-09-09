package org.taonity.gentooldataviewer.replay.controller

import org.taonity.gentooldataviewer.replay.dto.AdminAssignGentoolLinkBody
import org.taonity.gentooldataviewer.replay.dto.AdminDiscordUserOptionDto
import org.taonity.gentooldataviewer.replay.dto.AdminGentoolLinkDto
import org.taonity.gentooldataviewer.replay.dto.AdminGentoolPlayerOptionDto
import org.taonity.gentooldataviewer.replay.dto.AdminResolveDiscordUserBody
import org.taonity.gentooldataviewer.replay.service.GentoolLinkAdminService
import org.taonity.gentooldataviewer.security.principal.GoogleUserPrincipal
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/console/player-links")
class GentoolLinkAdminController(
    private val service: GentoolLinkAdminService,
) {
    @GetMapping("/users")
    fun searchUsers(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "20") size: Int,
    ): List<AdminDiscordUserOptionDto> = service.searchUsers(principal, q, size)

    @PostMapping("/users/resolve")
    fun resolveUser(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
        @RequestBody body: AdminResolveDiscordUserBody,
    ): AdminDiscordUserOptionDto = service.resolveDiscordUser(principal, body.discordUserId)

    @GetMapping("/players")
    fun searchPlayers(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "20") size: Int,
    ): List<AdminGentoolPlayerOptionDto> = service.searchPlayers(principal, q, size)

    @PutMapping
    fun assign(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
        @RequestBody body: AdminAssignGentoolLinkBody,
    ): AdminGentoolLinkDto = service.assign(principal, body.userId, body.playerId)

    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unlink(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
        @PathVariable userId: String,
    ) = service.unlink(principal, userId)
}