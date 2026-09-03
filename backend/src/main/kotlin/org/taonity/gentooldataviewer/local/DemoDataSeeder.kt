package org.taonity.gentooldataviewer.local

import io.github.oshai.kotlinlogging.KotlinLogging
import org.taonity.gentooldataviewer.common.demo.DemoDataContributor
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.annotation.Profile
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@Profile("demo-data")
class DemoDataSeeder(
    private val contributors: List<DemoDataContributor>,
) {
    @EventListener(ApplicationReadyEvent::class)
    fun seed() {
        val inserted = contributors.sumOf { contributor ->
            contributor.seed().also { count ->
                LOGGER.info { "Demo data: feature=${contributor.feature} inserted=$count" }
            }
        }
        LOGGER.info { "Demo data ready: contributors=${contributors.size} inserted=$inserted" }
    }

    private companion object {
        private val LOGGER = KotlinLogging.logger {}
    }
}
