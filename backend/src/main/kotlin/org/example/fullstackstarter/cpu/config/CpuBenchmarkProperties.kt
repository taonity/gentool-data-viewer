package org.example.fullstackstarter.cpu.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

@ConfigurationProperties(prefix = "app.cpu-benchmark")
data class CpuBenchmarkProperties(
    val baseUrl: URI,
    val maxPages: Int,
    val requestDelay: Duration,
    val requestTimeout: Duration,
    val userAgent: String,
    val scheduleEnabled: Boolean,
    val scheduleCron: String,
    val scheduleZone: String,
)