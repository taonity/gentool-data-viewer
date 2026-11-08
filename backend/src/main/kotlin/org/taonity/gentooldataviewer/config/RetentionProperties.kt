package org.taonity.gentooldataviewer.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.retention")
data class RetentionProperties(
    val audit: Policy,
    val replays: Policy = Policy(30, "0 0 4 * * *"),
) {
    init {
        require(replays.retentionDays in 1..3650) { "Replay retention must be between 1 and 3650 days" }
    }

    data class Policy(
        val retentionDays: Long,
        val cron: String,
    )
}
