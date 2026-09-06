package org.taonity.gentooldataviewer.replay.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

enum class GentoolLinkStatus { PENDING, APPROVED, REJECTED }

@Entity
@Table(name = "gentool_user_link")
class GentoolUserLinkEntity(
    @Id
    @Column(name = "user_id")
    val userId: String,
    @Column(name = "player_id", nullable = false, unique = true, length = 12)
    var playerId: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: GentoolLinkStatus = GentoolLinkStatus.PENDING,
    @Column(name = "requested_at", nullable = false)
    var requestedAt: Instant = Instant.now(),
    @Column(name = "decided_at")
    var decidedAt: Instant? = null,
    @Column(name = "decided_by_user_id")
    var decidedByUserId: String? = null,
)