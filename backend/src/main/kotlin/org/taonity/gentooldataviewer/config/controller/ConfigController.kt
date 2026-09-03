package org.taonity.gentooldataviewer.config.controller

import org.taonity.gentooldataviewer.config.dto.ConfigSchemaDto
import org.taonity.gentooldataviewer.config.dto.UpdateConfigBody
import org.taonity.gentooldataviewer.config.service.ConfigService
import org.taonity.gentooldataviewer.security.principal.GoogleUserPrincipal
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/console/config")
class ConfigController(
    private val configService: ConfigService,
) {
    @GetMapping
    fun schema(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
    ): ConfigSchemaDto = configService.getSchema(principal)

    @PutMapping
    fun update(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
        @RequestBody body: UpdateConfigBody,
    ): ConfigSchemaDto = configService.update(principal, body.values)

    @DeleteMapping("/{key}")
    fun reset(
        @AuthenticationPrincipal principal: GoogleUserPrincipal,
        @PathVariable key: String,
    ): ConfigSchemaDto = configService.reset(principal, key)
}
