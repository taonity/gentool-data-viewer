package org.taonity.gentooldataviewer.demo

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles
import org.taonity.gentooldataviewer.common.demo.DemoDataContributor
import org.taonity.gentooldataviewer.config.DynamicSchedulingConfig
import org.taonity.gentooldataviewer.replay.repository.ReplayRepository
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import java.time.Clock
import java.time.Instant

@SpringBootTest
@ActiveProfiles("demo")
class ScreenshotDemoProfileTest {
    @Autowired private lateinit var clock: Clock
    @Autowired private lateinit var replays: ReplayRepository
    @Autowired private lateinit var players: PlayerHardwareRepository
    @Autowired private lateinit var contributors: List<DemoDataContributor>
    @Autowired private lateinit var context: ApplicationContext

    @Test
    fun `screenshots use stable isolated fixtures without scheduled collection`() {
        assertThat(clock.instant()).isEqualTo(Instant.parse("2026-11-08T10:30:00Z"))
        assertThat(replays.count()).isEqualTo(50)
        assertThat(players.count()).isEqualTo(15)
        assertThat(replays.findAll()).allMatch { it.collectedAt == clock.instant().minusSeconds(600) }
        assertThat(replays.findAll().maxOf { it.matchAt }).isEqualTo(Instant.parse("2026-11-07T22:45:00Z"))
        assertThat(context.getBeansOfType(DynamicSchedulingConfig::class.java)).isEmpty()
        assertThat(contributors.sumOf { it.seed() }).isZero()
    }
}
