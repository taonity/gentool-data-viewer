package org.taonity.gentooldataviewer.replay.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "replay_player")
class ReplayPlayerEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    @Column(name = "replay_id", nullable = false)
    val replayId: String,
    @Column(name = "team_number", nullable = false)
    val teamNumber: Int,
    @Column(name = "slot_number", nullable = false)
    val slotNumber: Int,
    @Column(nullable = false)
    val address: String,
    @Column(nullable = false)
    val name: String,
    val army: String? = null,
)