package org.taonity.gentooldataviewer.replay.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class ReplayCollectionJobRecovery(
    private val jobService: ReplayCollectionJobService,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val recovered = jobService.recoverInterruptedJobs()
        if (recovered > 0) {
            LOGGER.warn { "Marked $recovered interrupted replay collection jobs as failed" }
        }
    }

    private companion object {
        private val LOGGER = KotlinLogging.logger {}
    }
}