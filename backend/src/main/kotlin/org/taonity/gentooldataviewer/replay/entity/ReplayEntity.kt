package org.taonity.gentooldataviewer.replay.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "replay")
class ReplayEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: String? = null,
    @Column(name = "source_url", nullable = false, unique = true, length = 2000)
    val sourceUrl: String,
    @Column(name = "source_date", nullable = false)
    val sourceDate: LocalDate,
    @Column(name = "reporter_id", nullable = false)
    val reporterId: String,
    @Column(name = "reporter_name", nullable = false)
    val reporterName: String,
    @Column(name = "player_names", nullable = false, length = 2000)
    val playerNames: String,
    @Column(name = "match_at", nullable = false)
    val matchAt: Instant,
    @Column(name = "windows_compat")
    val windowsCompat: String? = null,
    @Column(name = "gentool_version")
    val gentoolVersion: String? = null,
    @Column(name = "game_version")
    val gameVersion: String? = null,
    @Column(name = "install_type", length = 1000)
    val installType: String? = null,
    @Column(name = "rep_info_in_use")
    val repInfoInUse: String? = null,
    @Column(name = "map_name")
    val mapName: String? = null,
    @Column(name = "start_cash")
    val startCash: Int? = null,
    @Column(name = "match_type")
    val matchType: String? = null,
    @Column(name = "match_length_seconds")
    val matchLengthSeconds: Long? = null,
    @Column(name = "match_mode")
    val matchMode: String? = null,
    @Column(name = "system_info", length = 10000)
    val systemInfo: String? = null,
    @Column(name = "cpu", length = 500)
    val cpu: String? = null,
    @Column(name = "replay_file_name", length = 1000)
    val replayFileName: String? = null,
    @Column(name = "replay_size_bytes")
    val replaySizeBytes: Long? = null,
    @Column(name = "fields_json", nullable = false, length = 30000)
    val fieldsJson: String,
    @Column(name = "raw_text", nullable = false, length = 100000)
    val rawText: String,
    @Column(name = "collected_at", nullable = false)
    val collectedAt: Instant = Instant.now(),
)