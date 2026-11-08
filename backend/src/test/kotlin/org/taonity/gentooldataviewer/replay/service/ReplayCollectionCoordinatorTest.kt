package org.taonity.gentooldataviewer.replay.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import org.springframework.util.unit.DataSize
import org.taonity.gentooldataviewer.replay.config.ReplayCollectorProperties
import org.taonity.gentooldataviewer.replay.dto.ReplayCollectionWindowDto
import org.taonity.gentooldataviewer.replay.entity.CollectionTrigger
import java.net.URI
import java.time.Duration
import java.time.LocalDate
import java.util.concurrent.ExecutorService

class ReplayCollectionCoordinatorTest {
    private val collector = mock(ReplayCollector::class.java)
    private val jobs = mock(ReplayCollectionJobService::class.java)
    private val executor = mock(ExecutorService::class.java)
    private val policy = mock(ReplayRetentionPolicy::class.java)
    private val today = LocalDate.parse("2026-09-15")
    private val first = today.minusDays(29)
    private val service = ReplayCollectionCoordinator(
        ReplayCollectorProperties(URI.create("https://example.invalid/"), "zh", Duration.ZERO, Duration.ofSeconds(1),
            DataSize.ofKilobytes(64), 31, "test", false, "0 0 2 * * *", "UTC"),
        collector, jobs, executor, policy,
    )

    init {
        `when`(policy.window()).thenReturn(ReplayCollectionWindowDto(30, first, today))
    }

    @Test
    fun `manual scans outside retention or in future are rejected before queueing`() {
        listOf(first.minusDays(1) to first, today to today.plusDays(1), today to first).forEach { (start, end) ->
            assertThatThrownBy { service.startManual(start, end, "test", null) }
                .isInstanceOfSatisfying(ResponseStatusException::class.java) {
                    assertThat(it.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
                }
        }
        verifyNoInteractions(jobs, executor)
    }

    @Test
    fun `entire inclusive retention window is accepted`() {
        `when`(jobs.create(CollectionTrigger.MANUAL, first, today, "test", null, null)).thenReturn("job")
        assertThat(service.startManual(first, today, "test", null)).isEqualTo("job")
        verify(jobs).create(CollectionTrigger.MANUAL, first, today, "test", null, null)
    }

    @Test
    fun `user rescans cannot bypass retention`() {
        assertThatThrownBy { service.startUserRescan(first.minusDays(1), today, "test", "ABCDEF123456") }
            .isInstanceOf(ResponseStatusException::class.java).hasMessageContaining("retention")
        verifyNoInteractions(jobs, executor)
    }

    @Test
    fun `one day retention schedules today instead of expired yesterday`() {
        `when`(policy.window()).thenReturn(ReplayCollectionWindowDto(1, today, today))
        `when`(jobs.create(CollectionTrigger.SCHEDULED, today, today, "scheduler", null, null)).thenReturn("job")
        service.startScheduledYesterday()
        verify(jobs).create(CollectionTrigger.SCHEDULED, today, today, "scheduler", null, null)
    }
}
