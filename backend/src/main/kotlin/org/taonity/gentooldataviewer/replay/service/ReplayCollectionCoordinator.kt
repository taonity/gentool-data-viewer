package org.taonity.gentooldataviewer.replay.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.taonity.gentooldataviewer.replay.config.ReplayCollectorProperties
import org.taonity.gentooldataviewer.replay.entity.CollectionTrigger
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.ExecutorService

@Service
class ReplayCollectionCoordinator(
    private val properties: ReplayCollectorProperties,
    private val collector: ReplayCollector,
    private val jobService: ReplayCollectionJobService,
    private val replayCollectorExecutor: ExecutorService,
) {
    private val clock = Clock.systemUTC()

    fun startManual(startDate: LocalDate, endDate: LocalDate, requestedBy: String, userLimit: Int?): String {
        validateRange(startDate, endDate, userLimit)
        return start(CollectionTrigger.MANUAL, startDate, endDate, requestedBy, userLimit, null)
    }

    fun startUserRescan(startDate: LocalDate, endDate: LocalDate, requestedBy: String, targetPlayerId: String): String {
        validateRange(startDate, endDate, null)
        return start(CollectionTrigger.USER_RESCAN, startDate, endDate, requestedBy, null, targetPlayerId)
    }

    fun startScheduledYesterday() {
        val yesterday = LocalDate.now(clock).minusDays(1)
        try {
            start(CollectionTrigger.SCHEDULED, yesterday, yesterday, "scheduler", null, null)
        } catch (error: Exception) {
            LOGGER.error(error) { "Could not queue scheduled replay collection" }
        }
    }

    private fun start(
        trigger: CollectionTrigger,
        startDate: LocalDate,
        endDate: LocalDate,
        requestedBy: String,
        userLimit: Int?,
        targetPlayerId: String?,
    ): String {
        val jobId = jobService.create(trigger, startDate, endDate, requestedBy, userLimit, targetPlayerId)
        replayCollectorExecutor.submit { run(jobId, startDate, endDate, userLimit, targetPlayerId) }
        return jobId
    }

    private fun run(
        jobId: String,
        startDate: LocalDate,
        endDate: LocalDate,
        userLimit: Int?,
        targetPlayerId: String?,
    ) {
        var latestProgress = CollectionProgress()
        try {
            jobService.markRunning(jobId)
            latestProgress = collector.collect(startDate, endDate, userLimit, targetPlayerId) {
                latestProgress = it
                jobService.updateProgress(jobId, it)
            }
            jobService.complete(jobId, latestProgress)
        } catch (error: Exception) {
            LOGGER.error(error) { "Replay collection job $jobId failed" }
            jobService.fail(jobId, latestProgress, error)
        }
    }

    private fun validateRange(startDate: LocalDate, endDate: LocalDate, userLimit: Int?) {
        if (endDate.isBefore(startDate)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "endDate must not be before startDate")
        }
        val days = ChronoUnit.DAYS.between(startDate, endDate) + 1
        if (days > properties.maxDaysPerJob) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "A job can collect at most ${properties.maxDaysPerJob} days")
        }
        if (endDate.isAfter(LocalDate.now(clock))) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Future dates cannot be collected")
        }
        if (userLimit != null && userLimit < 1) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "userLimit must be greater than zero")
        }
    }

    private companion object {
        private val LOGGER = KotlinLogging.logger {}
    }
}