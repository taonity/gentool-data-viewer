package org.taonity.gentooldataviewer.replay.service

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@TestConfiguration
class ReplayRetentionTestClock {
    @Bean
    @Primary
    fun fixedReplayRetentionClock(): Clock = Clock.fixed(Instant.parse("2026-09-15T12:00:00Z"), ZoneOffset.UTC)
}
