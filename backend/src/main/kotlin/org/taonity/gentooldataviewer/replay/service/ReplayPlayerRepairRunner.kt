package org.taonity.gentooldataviewer.replay.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class ReplayPlayerRepairRunner(
    private val importService: ReplayImportService,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments) {
        val repaired = importService.repairMissingPlayers()
        if (repaired > 0) LOGGER.info { "Repaired player data for $repaired replays" }
    }

    private companion object {
        val LOGGER = KotlinLogging.logger {}
    }
}