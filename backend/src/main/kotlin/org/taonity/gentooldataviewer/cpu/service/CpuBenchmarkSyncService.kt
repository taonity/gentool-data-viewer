package org.taonity.gentooldataviewer.cpu.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.taonity.gentooldataviewer.cpu.client.PassMarkClient
import org.taonity.gentooldataviewer.cpu.repository.CpuBenchmarkRepository
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

data class CpuBenchmarkSyncSummary(
    val benchmarks: Int,
    val fetchedAt: Instant,
    val ratings: RatingSummary,
)

@Service
class CpuBenchmarkSyncService(
    private val client: PassMarkClient,
    private val catalogService: CpuBenchmarkCatalogService,
    private val ratingService: CpuRatingService,
    private val benchmarkRepository: CpuBenchmarkRepository,
) {
    private val running = AtomicBoolean(false)

    fun sync(): CpuBenchmarkSyncSummary {
        check(running.compareAndSet(false, true)) { "CPU benchmark refresh is already running" }
        try {
            val records = client.fetchAll()
            val fetchedAt = Instant.now()
            catalogService.replace(records, fetchedAt)
            val ratings = ratingService.rateAll()
            LOGGER.info { "Refreshed ${records.size} PassMark Thread Mark records; rated ${ratings.rated}/${ratings.total} players" }
            return CpuBenchmarkSyncSummary(records.size, fetchedAt, ratings)
        } finally {
            running.set(false)
        }
    }

    fun scheduledSync() {
        runCatching(::sync).onFailure { LOGGER.error(it) { "Scheduled CPU benchmark refresh failed" } }
    }

    fun latestFetchedAt(): Instant? = benchmarkRepository.findTopByOrderByFetchedAtDesc()?.fetchedAt

    private companion object {
        private val LOGGER = KotlinLogging.logger {}
    }
}