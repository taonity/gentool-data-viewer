package org.example.fullstackstarter.replay.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.util.unit.DataSize
import java.net.URI
import java.time.Duration

@ConfigurationProperties(prefix = "app.replay-collector")
data class ReplayCollectorProperties(
    val baseUrl: URI,
    val game: String,
    val requestDelay: Duration,
    val requestTimeout: Duration,
    val maxTextFileSize: DataSize,
    val maxDaysPerJob: Long,
    val userAgent: String,
    val scheduleEnabled: Boolean,
    val scheduleCron: String,
    val scheduleZone: String,
)