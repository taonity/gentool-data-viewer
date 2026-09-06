package org.taonity.gentooldataviewer.security.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.discord")
data class DiscordProperties(
    val cdnBaseUrl: String,
)