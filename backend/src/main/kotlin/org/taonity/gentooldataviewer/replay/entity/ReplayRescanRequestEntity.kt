package org.taonity.gentooldataviewer.replay.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "replay_rescan_request")
class ReplayRescanRequestEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    @Column(name = "requested_by_user_id", nullable = false)
    val requestedByUserId: String,
    @Column(name = "target_player_id", nullable = false, length = 12)
    val targetPlayerId: String,
    @Column(name = "own_target", nullable = false)
    val ownTarget: Boolean,
    @Column(name = "job_id", nullable = false)
    val jobId: String,
    @Column(name = "requested_at", nullable = false)
    val requestedAt: Instant = Instant.now(),
)