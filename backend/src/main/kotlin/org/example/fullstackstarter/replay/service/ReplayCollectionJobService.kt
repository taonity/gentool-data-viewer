package org.example.fullstackstarter.replay.service

import org.example.fullstackstarter.replay.dto.ReplayCollectionJobDto
import org.example.fullstackstarter.replay.entity.CollectionStatus
import org.example.fullstackstarter.replay.entity.CollectionTrigger
import org.example.fullstackstarter.replay.entity.ReplayCollectionJobEntity
import org.example.fullstackstarter.replay.repository.ReplayCollectionJobRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate

@Service
class ReplayCollectionJobService(
    private val repository: ReplayCollectionJobRepository,
) {
    @Transactional
    fun create(trigger: CollectionTrigger, startDate: LocalDate, endDate: LocalDate, requestedBy: String, userLimit: Int?): String =
        requireNotNull(
            repository.save(
                ReplayCollectionJobEntity(
                    triggerType = trigger,
                    startDate = startDate,
                    endDate = endDate,
                    requestedBy = requestedBy,
                    userLimit = userLimit,
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
        job.status = CollectionStatus.COMPLETED
        job.finishedAt = Instant.now()
    }

    @Transactional
    fun fail(id: String, progress: CollectionProgress, error: Throwable) {
        updateProgress(id, progress)
        val job = repository.getReferenceById(id)
        job.status = CollectionStatus.FAILED
        job.finishedAt = Instant.now()
        job.errorMessage = (error.message ?: error.javaClass.simpleName).take(2000)
    }

    @Transactional(readOnly = true)
    fun latest(): List<ReplayCollectionJobDto> =
        repository.findTop20ByOrderByCreatedAtDesc().map(ReplayCollectionJobDto::from)
}