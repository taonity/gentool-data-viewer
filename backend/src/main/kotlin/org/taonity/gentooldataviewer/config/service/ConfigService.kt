package org.taonity.gentooldataviewer.config.service

import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.config.ConfigField
import org.taonity.gentooldataviewer.config.ConfigRegistry
import org.taonity.gentooldataviewer.config.ConfigValidationException
import org.taonity.gentooldataviewer.config.dto.ConfigFieldDto
import org.taonity.gentooldataviewer.config.dto.ConfigSchemaDto
import org.taonity.gentooldataviewer.config.entity.ConfigOverrideEntity
import org.taonity.gentooldataviewer.config.repository.ConfigOverrideRepository
import org.taonity.gentooldataviewer.console.entity.AuditAction
import org.taonity.gentooldataviewer.console.exception.ConsoleNotFoundException
import org.taonity.gentooldataviewer.console.service.AccessGuard
import org.taonity.gentooldataviewer.console.service.AuditService
import org.taonity.gentooldataviewer.security.principal.GoogleUserPrincipal
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import java.time.Instant

@Service
class ConfigService(
    private val registry: ConfigRegistry,
    private val settings: AppSettings,
    private val overrideRepository: ConfigOverrideRepository,
    private val accessGuard: AccessGuard,
    private val auditService: AuditService,
) {
    fun getSchema(principal: GoogleUserPrincipal): ConfigSchemaDto {
        accessGuard.requireView(principal)
        return buildSchema()
    }

    @Transactional
    fun update(principal: GoogleUserPrincipal, values: Map<String, JsonNode>): ConfigSchemaDto {
        val actor = accessGuard.requireOwner(principal)
        if (values.isEmpty()) throw ConfigValidationException("No values provided")

        var effective = settings.effective()
        val parsed = LinkedHashMap<String, Pair<ConfigField, Any>>()
        for ((key, node) in values) {
            val field = registry.field(key) ?: throw ConfigValidationException("Unknown config key '$key'")
            val typed = registry.parse(field, node, effective)
            effective = field.apply(effective, typed)
            parsed[key] = field to typed
        }

        val now = Instant.now()
        for ((key, pair) in parsed) {
            val json = registry.serialize(pair.second)
            val existing = overrideRepository.findById(key).orElse(null)
            if (existing != null) {
                existing.valueJson = json
                existing.updatedAt = now
                existing.updatedBy = actor.email
                overrideRepository.save(existing)
            } else {
                overrideRepository.save(
                    ConfigOverrideEntity(configKey = key, valueJson = json, updatedAt = now, updatedBy = actor.email),
                )
            }
            auditService.record(AuditAction.EDIT_CONFIG, "config_override", key, actor)
        }
        settings.reload()
        return buildSchema()
    }

    @Transactional
    fun reset(principal: GoogleUserPrincipal, key: String): ConfigSchemaDto {
        val actor = accessGuard.requireOwner(principal)
        registry.field(key) ?: throw ConsoleNotFoundException("Unknown config key '$key'")
        if (overrideRepository.existsById(key)) {
            overrideRepository.deleteById(key)
            auditService.record(AuditAction.RESET_CONFIG, "config_override", key, actor)
            settings.reload()
        }
        return buildSchema()
    }

    private fun buildSchema(): ConfigSchemaDto {
        val defaults = settings.defaults()
        val effective = settings.effective()
        val overriddenKeys = overrideRepository.findAll().mapTo(HashSet()) { it.configKey }
        val fields = registry.fields().map { field ->
            ConfigFieldDto(
                key = field.key,
                group = field.group,
                label = field.label,
                type = field.type.name,
                min = field.min,
                max = field.max,
                enumValues = field.enumValues(effective),
                defaultValue = field.read(defaults),
                value = field.read(effective),
                overridden = field.key in overriddenKeys,
            )
        }
        return ConfigSchemaDto(fields)
    }
}
