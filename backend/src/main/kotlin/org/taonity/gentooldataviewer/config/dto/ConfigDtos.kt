package org.taonity.gentooldataviewer.config.dto

import tools.jackson.databind.JsonNode

data class ConfigFieldDto(
    val key: String,
    val group: String,
    val label: String,
    val type: String,
    val min: Double?,
    val max: Double?,
    val enumValues: List<String>,
    val defaultValue: Any?,
    val value: Any?,
    val overridden: Boolean,
)

data class ConfigSchemaDto(
    val fields: List<ConfigFieldDto>,
)

data class UpdateConfigBody(
    val values: Map<String, JsonNode>,
)
