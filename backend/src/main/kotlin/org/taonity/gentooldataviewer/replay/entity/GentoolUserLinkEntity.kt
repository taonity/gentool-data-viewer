package org.taonity.gentooldataviewer.replay.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.io.Serializable
import java.time.Instant

enum class GentoolLinkStatus { PENDING, APPROVED, REJECTED }

@Entity
@Table(
    name = "gentool_user_link",
    uniqueConstraints = [UniqueConstraint(name = "uk_gentool_user_link_player", columnNames = ["player_id"])],
)
@IdClass(GentoolUserLinkId::class)
class GentoolUserLinkEntity(
    @Id
    @Column(name = "user_id")
    val userId: String,
    @Id
    @Column(name = "player_id", nullable = false, unique = true, length = 12)
    val playerId: String,
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

data class GentoolUserLinkId(
    val userId: String = "",
    val playerId: String = "",
) : Serializable