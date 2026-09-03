package org.taonity.gentooldataviewer.config

import org.taonity.gentooldataviewer.console.config.ConsolePagingProperties

data class EffectiveConfig(
    val retention: RetentionProperties,
    val console: ConsolePagingProperties,
)

enum class ConfigType {
    BOOL,
    INT,
    LONG,
    DOUBLE,
    STRING,
    TEXT,
    ENUM,
    STRING_LIST,
}

class ConfigField(
    val key: String,
    val group: String,
    val type: ConfigType,
    val min: Double? = null,
    val max: Double? = null,
    val enumValues: (EffectiveConfig) -> List<String> = { emptyList() },
    val read: (EffectiveConfig) -> Any?,
    val apply: (EffectiveConfig, Any) -> EffectiveConfig,
    val validate: (Any) -> Unit = {},
) {
    val label: String = key.substringAfterLast('.')
}

class ConfigValidationException(message: String) : RuntimeException(message)
