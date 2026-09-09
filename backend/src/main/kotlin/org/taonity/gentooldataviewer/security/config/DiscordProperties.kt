package org.taonity.gentooldataviewer.security.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

@ConfigurationProperties(prefix = "app.discord")
data class DiscordProperties(
    val cdnBaseUrl: String,
    val botApiBaseUrl: URI,
    val botToken: String,
    val botRequestTimeout: Duration,
    val botUserAgent: String,
)