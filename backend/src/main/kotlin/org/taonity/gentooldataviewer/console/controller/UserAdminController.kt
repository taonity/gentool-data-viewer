package org.taonity.gentooldataviewer.console.controller

import org.taonity.gentooldataviewer.console.dto.ChangeRoleBody
import org.taonity.gentooldataviewer.console.dto.UserSummaryDto
import org.taonity.gentooldataviewer.console.service.AccessService
import org.taonity.gentooldataviewer.security.principal.GoogleUserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/console/users")
class UserAdminController(
    private val accessService: AccessService,
) {
    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
    ): List<UserSummaryDto> = accessService.listUsers(principal)

    @PutMapping("/{googleId}/role")
    fun changeRole(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
        @PathVariable googleId: String,
        @RequestBody body: ChangeRoleBody,
    ): UserSummaryDto = accessService.changeRole(principal, googleId, body.role)
}
