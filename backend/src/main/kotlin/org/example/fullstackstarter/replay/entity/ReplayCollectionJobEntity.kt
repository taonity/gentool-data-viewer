package org.example.fullstackstarter.replay.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

enum class CollectionTrigger { SCHEDULED, MANUAL }
enum class CollectionStatus { QUEUED, RUNNING, COMPLETED, FAILED }

@Entity
@Table(name = "replay_collection_job")
class ReplayCollectionJobEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", nullable = false)
    val triggerType: CollectionTrigger,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: CollectionStatus = CollectionStatus.QUEUED,
    @Column(name = "start_date", nullable = false)
    val startDate: LocalDate,
    @Column(name = "end_date", nullable = false)
    val endDate: LocalDate,
    @Column(name = "requested_by", nullable = false)
    val requestedBy: String,
    @Column(name = "user_limit")
    val userLimit: Int? = null,
    @Column(name = "created_at", nullable = false)
    val createdAt: Instant = Instant.now(),
    @Column(name = "started_at")
    var startedAt: Instant? = null,
    @Column(name = "finished_at")
    var finishedAt: Instant? = null,
    @Column(name = "directories_discovered", nullable = false)
    var directoriesDiscovered: Long = 0,
    @Column(name = "directories_scanned", nullable = false)
    var directoriesScanned: Long = 0,
    @Column(name = "files_discovered", nullable = false)
    var filesDiscovered: Long = 0,
    @Column(name = "files_imported", nullable = false)
    var filesImported: Long = 0,
    @Column(name = "files_skipped", nullable = false)
    var filesSkipped: Long = 0,
    @Column(nullable = false)
    var failures: Long = 0,
    @Column(name = "error_message", length = 2000)
    var errorMessage: String? = null,
)