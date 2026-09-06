package org.taonity.gentooldataviewer.replay.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.replay-rescan")
data class ReplayRescanProperties(
    val lookbackDays: Long,
    val otherDailyLimit: Long,
    val targetCooldown: Duration,
    val privilegedOtherDailyLimit: Long,
    val privilegedTargetCooldown: Duration,
)