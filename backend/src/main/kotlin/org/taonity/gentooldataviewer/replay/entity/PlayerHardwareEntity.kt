package org.taonity.gentooldataviewer.replay.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.taonity.gentooldataviewer.cpu.service.CpuMatchStatus
import java.time.Instant

@Entity
@Table(name = "player_hardware")
class PlayerHardwareEntity(
    @Id
    @Column(name = "player_id")
    val playerId: String,
    @Column(name = "latest_name", nullable = false)
    var latestName: String,
    @Column(name = "main_name", nullable = false)
    var mainName: String = latestName,
    @Column(name = "aliases_json", nullable = false, length = 5000)
    var aliasesJson: String = "[]",
    @Column(name = "replay_count", nullable = false)
    var replayCount: Long = 0,
    @Column(length = 500)
    var cpu: String? = null,
    @Column(name = "cpu_score")
    var cpuScore: Int? = null,
    @Column(name = "cpu_benchmark_id")
    var cpuBenchmarkId: String? = null,
    @Column(name = "cpu_benchmark_name", length = 500)
    var cpuBenchmarkName: String? = null,
    @Column(name = "cpu_benchmark_url", length = 1000)
    var cpuBenchmarkUrl: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "cpu_match_status", nullable = false)
    var cpuMatchStatus: CpuMatchStatus = if (cpu == null) CpuMatchStatus.NO_CPU else CpuMatchStatus.UNMATCHED,
    @Column(name = "cpu_score_updated_at")
    var cpuScoreUpdatedAt: Instant? = null,
    @Column(name = "system_info", length = 10000)
    var systemInfo: String? = null,
    @Column(name = "observed_at", nullable = false)
    var observedAt: Instant,
    @Column(name = "source_replay_id", nullable = false)
    var sourceReplayId: String,
)