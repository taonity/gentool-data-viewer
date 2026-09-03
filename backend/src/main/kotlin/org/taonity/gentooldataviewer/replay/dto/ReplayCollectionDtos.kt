package org.taonity.gentooldataviewer.replay.dto

import org.taonity.gentooldataviewer.replay.entity.CollectionStatus
import org.taonity.gentooldataviewer.replay.entity.CollectionTrigger
import org.taonity.gentooldataviewer.replay.entity.ReplayCollectionJobEntity
import java.time.Instant
import java.time.LocalDate

data class StartReplayCollectionRequest(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val userLimit: Int? = null,
)

data class ReplayCollectionJobDto(
    val id: String,
    val triggerType: CollectionTrigger,
    val status: CollectionStatus,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val requestedBy: String,
    val userLimit: Int?,
    val createdAt: Instant,
    val startedAt: Instant?,
    val finishedAt: Instant?,
    val directoriesDiscovered: Long,
    val directoriesScanned: Long,
    val filesDiscovered: Long,
    val filesImported: Long,
    val filesSkipped: Long,
    val failures: Long,
    val errorMessage: String?,
) {
    companion object {
        fun from(entity: ReplayCollectionJobEntity) = ReplayCollectionJobDto(
            id = requireNotNull(entity.id),
            triggerType = entity.triggerType,
            status = entity.status,
            startDate = entity.startDate,
            endDate = entity.endDate,
            requestedBy = entity.requestedBy,
            userLimit = entity.userLimit,
            createdAt = entity.createdAt,
            startedAt = entity.startedAt,
            finishedAt = entity.finishedAt,
            directoriesDiscovered = entity.directoriesDiscovered,
            directoriesScanned = entity.directoriesScanned,
            filesDiscovered = entity.filesDiscovered,
            filesImported = entity.filesImported,
            filesSkipped = entity.filesSkipped,
            failures = entity.failures,
            errorMessage = entity.errorMessage,
        )
    }
}