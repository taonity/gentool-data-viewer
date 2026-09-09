package org.taonity.gentooldataviewer.config

import org.springframework.scheduling.support.CronExpression
import org.springframework.stereotype.Component
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper

@Component
class ConfigRegistry(
    private val objectMapper: ObjectMapper,
) {
    private val allFields: List<ConfigField> = buildFields()
    private val byKey: Map<String, ConfigField> = allFields.associateBy { it.key }

    fun fields(): List<ConfigField> = allFields

    fun field(key: String): ConfigField? = byKey[key]

    fun parse(field: ConfigField, node: JsonNode, effective: EffectiveConfig): Any {
        val value: Any = try {
            when (field.type) {
                ConfigType.BOOL -> objectMapper.treeToValue(node, java.lang.Boolean::class.java) as Boolean
                ConfigType.INT -> objectMapper.treeToValue(node, Integer::class.java) as Int
                ConfigType.LONG -> objectMapper.treeToValue(node, java.lang.Long::class.java) as Long
                ConfigType.DOUBLE -> objectMapper.treeToValue(node, java.lang.Double::class.java) as Double
                ConfigType.STRING, ConfigType.TEXT, ConfigType.ENUM ->
                    objectMapper.treeToValue(node, String::class.java)
                ConfigType.STRING_LIST -> objectMapper.convertValue(node, STRING_LIST_TYPE)
            }
        } catch (e: Exception) {
            throw ConfigValidationException("Invalid value for '${field.key}': expected ${field.type}")
        }

        when (field.type) {
            ConfigType.INT, ConfigType.LONG, ConfigType.DOUBLE -> validateRange(field, (value as Number).toDouble())
            ConfigType.ENUM -> {
                val allowed = field.enumValues(effective)
                if (value !in allowed) {
                    throw ConfigValidationException("Invalid value for '${field.key}': must be one of $allowed")
                }
            }
            ConfigType.STRING, ConfigType.TEXT -> {
                if ((value as String).isBlank()) {
                    throw ConfigValidationException("Invalid value for '${field.key}': must not be blank")
                }
            }
            else -> Unit
        }
        field.validate(value)
        return value
    }

    fun serialize(value: Any): String = objectMapper.writeValueAsString(value)

    fun parseStored(field: ConfigField, valueJson: String, effective: EffectiveConfig): Any =
        parse(field, objectMapper.readTree(valueJson), effective)

    private fun validateRange(field: ConfigField, value: Double) {
        field.min?.let { if (value < it) throw ConfigValidationException("'${field.key}' must be >= ${format(it)}") }
        field.max?.let { if (value > it) throw ConfigValidationException("'${field.key}' must be <= ${format(it)}") }
    }

    private fun format(d: Double): String = if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

    private fun buildFields(): List<ConfigField> {
        val fields = mutableListOf<ConfigField>()

        // ---- Retention ----
        fields += ConfigField(
            key = "app.retention.audit.retention-days", group = "Retention", type = ConfigType.LONG, min = 1.0, max = 3650.0,
            read = { it.retention.audit.retentionDays },
            apply = { c, v -> c.copy(retention = c.retention.copy(audit = c.retention.audit.copy(retentionDays = v as Long))) },
        )
        fields += ConfigField(
            key = "app.retention.audit.cron", group = "Retention", type = ConfigType.STRING,
            read = { it.retention.audit.cron },
            apply = { c, v -> c.copy(retention = c.retention.copy(audit = c.retention.audit.copy(cron = v as String))) },
            validate = { v -> requireValidCron(v as String) },
        )

        // ---- Console ----
        fields += ConfigField(
            key = "app.console.max-page-size", group = "Console", type = ConfigType.INT, min = 10.0, max = 1000.0,
            read = { it.console.maxPageSize },
            apply = { c, v -> c.copy(console = c.console.copy(maxPageSize = v as Int)) },
        )
        fields += ConfigField(
            key = "app.console.access-requests-enabled", group = "Console", type = ConfigType.BOOL,
            read = { it.console.accessRequestsEnabled },
            apply = { c, v -> c.copy(console = c.console.copy(accessRequestsEnabled = v as Boolean)) },
        )

        // ---- Replay rescan ----
        fields += ConfigField(
            key = "app.replay-rescan.max-linked-players", group = "Replay rescan", type = ConfigType.INT,
            min = 1.0, max = 10.0,
            read = { it.replayRescan.maxLinkedPlayers },
            apply = { c, v -> c.copy(replayRescan = c.replayRescan.copy(maxLinkedPlayers = v as Int)) },
        )

        return fields
    }

    private companion object {
        private val STRING_LIST_TYPE = object : TypeReference<List<String>>() {}

        private fun requireValidCron(expression: String) {
            if (!CronExpression.isValidExpression(expression)) {
                throw ConfigValidationException("Invalid cron expression: '$expression'")
            }
        }
    }
}
