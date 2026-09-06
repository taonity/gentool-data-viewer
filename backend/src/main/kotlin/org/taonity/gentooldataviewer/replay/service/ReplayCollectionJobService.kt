package org.taonity.gentooldataviewer.replay.service

import org.taonity.gentooldataviewer.replay.dto.ReplayCollectionJobDto
import org.taonity.gentooldataviewer.replay.entity.CollectionStatus
import org.taonity.gentooldataviewer.replay.entity.CollectionTrigger
import org.taonity.gentooldataviewer.replay.entity.ReplayCollectionJobEntity
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayCollectionJobRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@Service
class ReplayCollectionJobService(
    private val repository: ReplayCollectionJobRepository,
    private val playerHardwareRepository: PlayerHardwareRepository,
) {
    @Transactional
    fun create(
        trigger: CollectionTrigger,
        startDate: LocalDate,
        endDate: LocalDate,
        requestedBy: String,
        userLimit: Int?,
        targetPlayerId: String? = null,
    ): String =
        requireNotNull(
            repository.save(
                ReplayCollectionJobEntity(
                    triggerType = trigger,
                    startDate = startDate,
                    endDate = endDate,
                    requestedBy = requestedBy,
                    userLimit = userLimit,
                    targetPlayerId = targetPlayerId,
                ),
            ).id,
        )

    @Transactional
    fun markRunning(id: String) {
        val job = repository.getReferenceById(id)
        job.status = CollectionStatus.RUNNING
        job.startedAt = Instant.now()
    }

    @Transactional
    fun updateProgress(id: String, progress: CollectionProgress) {
        val job = repository.getReferenceById(id)
        job.directoriesDiscovered = progress.directoriesDiscovered
        job.directoriesScanned = progress.directoriesScanned
        job.filesDiscovered = progress.filesDiscovered
        job.filesImported = progress.filesImported
        job.filesSkipped = progress.filesSkipped
        job.failures = progress.failures
    }

    @Transactional
    fun complete(id: String, progress: CollectionProgress) {
        updateProgress(id, progress)
        val job = repository.getReferenceById(id)
        val finishedAt = Instant.now()
        job.status = CollectionStatus.COMPLETED
        job.finishedAt = finishedAt
        val targetPlayerId = job.targetPlayerId
        if (job.triggerType == CollectionTrigger.USER_RESCAN && targetPlayerId != null) {
            playerHardwareRepository.findById(targetPlayerId).ifPresent { player ->
                player.gentoolRefreshedAt = finishedAt
            }
        }
    }

    @Transactional
    fun fail(id: String, progress: CollectionProgress, error: Throwable) {
        updateProgress(id, progress)
        val job = repository.getReferenceById(id)
        job.status = CollectionStatus.FAILED
        job.finishedAt = Instant.now()
        job.errorMessage = (error.message ?: error.javaClass.simpleName).take(2000)
    }

    @Transactional
    fun recoverInterruptedJobs(): Int {
        val jobs = repository.findAllByStatusIn(setOf(CollectionStatus.QUEUED, CollectionStatus.RUNNING))
        if (jobs.isEmpty()) return 0
        val finishedAt = Instant.now()
        jobs.forEach { job ->
            job.status = CollectionStatus.FAILED
            job.finishedAt = finishedAt
            job.errorMessage = "Interrupted by application restart"
        }
        repository.saveAll(jobs)
        return jobs.size
    }

    @Transactional(readOnly = true)
    fun latest(): List<ReplayCollectionJobDto> =
        repository.findTop20ByOrderByCreatedAtDesc().map(ReplayCollectionJobDto::from)
}