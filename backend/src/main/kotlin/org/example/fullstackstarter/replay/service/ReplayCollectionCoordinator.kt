package org.example.fullstackstarter.replay.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.example.fullstackstarter.replay.config.ReplayCollectorProperties
import org.example.fullstackstarter.replay.entity.CollectionTrigger
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean

@Service
class ReplayCollectionCoordinator(
    private val properties: ReplayCollectorProperties,
    private val collector: ReplayCollector,
    private val jobService: ReplayCollectionJobService,
    private val replayCollectorExecutor: ExecutorService,
) {
    private val running = AtomicBoolean(false)
    private val clock = Clock.systemUTC()

    fun startManual(startDate: LocalDate, endDate: LocalDate, requestedBy: String, userLimit: Int?): String {
        validateRange(startDate, endDate, userLimit)
        return start(CollectionTrigger.MANUAL, startDate, endDate, requestedBy, userLimit)
    }

    fun startScheduledYesterday() {
        val yesterday = LocalDate.now(clock).minusDays(1)
        try {
            start(CollectionTrigger.SCHEDULED, yesterday, yesterday, "scheduler", null)
        } catch (error: ResponseStatusException) {
            LOGGER.info { "Skipping scheduled replay collection because another job is active" }
        }
    }

    private fun start(
        trigger: CollectionTrigger,
        startDate: LocalDate,
        endDate: LocalDate,
        requestedBy: String,
        userLimit: Int?,
    ): String {
        if (!running.compareAndSet(false, true)) {
            throw ResponseStatusException(HttpStatus.CONFLICT, "A replay collection job is already active")
        }
        val jobId = try {
            jobService.create(trigger, startDate, endDate, requestedBy, userLimit)
        } catch (error: Exception) {
            running.set(false)
            throw error
        }
        replayCollectorExecutor.submit { run(jobId, startDate, endDate, userLimit) }
        return jobId
    }

    private fun run(jobId: String, startDate: LocalDate, endDate: LocalDate, userLimit: Int?) {
        var latestProgress = CollectionProgress()
        try {
            jobService.markRunning(jobId)
            latestProgress = collector.collect(startDate, endDate, userLimit) {
                latestProgress = it
                jobService.updateProgress(jobId, it)
            }
            jobService.complete(jobId, latestProgress)
        } catch (error: Exception) {
            LOGGER.error(error) { "Replay collection job $jobId failed" }
            jobService.fail(jobId, latestProgress, error)
        } finally {
            running.set(false)
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