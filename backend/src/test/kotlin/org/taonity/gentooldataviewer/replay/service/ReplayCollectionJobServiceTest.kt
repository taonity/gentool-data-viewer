package org.taonity.gentooldataviewer.replay.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.taonity.gentooldataviewer.replay.entity.CollectionStatus
import org.taonity.gentooldataviewer.replay.entity.CollectionTrigger
import org.taonity.gentooldataviewer.replay.entity.PlayerHardwareEntity
import org.taonity.gentooldataviewer.replay.entity.ReplayCollectionJobEntity
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayCollectionJobRepository
import java.time.Instant
import java.time.LocalDate
import java.util.Optional

class ReplayCollectionJobServiceTest {
    private val jobRepository = mock(ReplayCollectionJobRepository::class.java)
    private val playerRepository = mock(PlayerHardwareRepository::class.java)
    private val service = ReplayCollectionJobService(jobRepository, playerRepository)

    @Test
    fun `completed user rescan records player refresh time`() {
        val job = userRescanJob()
        val player = player()
        `when`(jobRepository.getReferenceById("job-1")).thenReturn(job)
        `when`(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player))

        service.complete("job-1", CollectionProgress())

        assertThat(job.status).isEqualTo(CollectionStatus.COMPLETED)
        assertThat(player.gentoolRefreshedAt).isEqualTo(job.finishedAt)
    }

    @Test
    fun `failed user rescan does not record player refresh time`() {
        val job = userRescanJob()
        val player = player()
        `when`(jobRepository.getReferenceById("job-1")).thenReturn(job)
        `when`(playerRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player))

        service.fail("job-1", CollectionProgress(), IllegalStateException("failed"))

        assertThat(job.status).isEqualTo(CollectionStatus.FAILED)
        assertThat(player.gentoolRefreshedAt).isNull()
    }

    private fun userRescanJob() = ReplayCollectionJobEntity(
        triggerType = CollectionTrigger.USER_RESCAN,
        startDate = LocalDate.now(),
        endDate = LocalDate.now(),
        requestedBy = "viewer@example.com",
        targetPlayerId = PLAYER_ID,
    )

    private fun player() = PlayerHardwareEntity(
        playerId = PLAYER_ID,
        latestName = "Player",
        observedAt = Instant.parse("2026-09-01T12:00:00Z"),
        sourceReplayId = "replay-1",
    )

    private companion object {
        const val PLAYER_ID = "ABCDEF123456"
    }
}