package org.taonity.gentooldataviewer.replay.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "replay_associated_file")
class ReplayAssociatedFileEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    @Column(name = "replay_id", nullable = false)
    val replayId: String,
    @Column(name = "file_name", nullable = false, length = 1000)
    val fileName: String,
    @Column(name = "size_bytes", nullable = false)
    val sizeBytes: Long,
)