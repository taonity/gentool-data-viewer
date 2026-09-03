package org.taonity.gentooldataviewer.demo

import org.assertj.core.api.Assertions.assertThat
import org.taonity.gentooldataviewer.common.demo.DemoDataContributor
import org.taonity.gentooldataviewer.console.repository.AuditLogRepository
import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.user.repository.UserRepository
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayAssociatedFileRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayPlayerRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRepository
import org.taonity.gentooldataviewer.cpu.repository.CpuBenchmarkRepository
import org.taonity.gentooldataviewer.cpu.service.CpuMatchStatus
import org.taonity.gentooldataviewer.cpu.service.CPU_PLAYER_SORT_PROPERTIES
import org.taonity.gentooldataviewer.replay.service.REPLAY_SORT_PROPERTIES
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@SpringBootTest
@ActiveProfiles("h2", "demo-data")
class DemoDataProfileTest {
    @Autowired
    private lateinit var contributors: List<DemoDataContributor>

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    @Autowired
    private lateinit var replayRepository: ReplayRepository

    @Autowired
    private lateinit var replayPlayerRepository: ReplayPlayerRepository

    @Autowired
    private lateinit var replayAssociatedFileRepository: ReplayAssociatedFileRepository

    @Autowired
    private lateinit var playerHardwareRepository: PlayerHardwareRepository

    @Autowired
    private lateinit var cpuBenchmarkRepository: CpuBenchmarkRepository

    @Test
    fun `demo profile seeds feature data idempotently`() {
        val pending = userRepository.findByAccessStatusOrderByEmailAsc(AccessRequestStatus.PENDING)
        assertThat(pending).hasSize(2)
        assertThat(pending.map { it.requestedRole }).containsExactly(ConsoleRole.VIEWER, ConsoleRole.EDITOR)

        assertThat(auditLogRepository.count()).isEqualTo(5)
        assertThat(auditLogRepository.existsByActorGoogleId("demo-data-owner")).isTrue()

        assertThat(replayRepository.count()).isEqualTo(50)
        assertThat(playerHardwareRepository.count()).isEqualTo(15)
        assertThat(replayPlayerRepository.count()).isEqualTo(166)
        assertThat(replayAssociatedFileRepository.count()).isEqualTo(100)
        assertThat(cpuBenchmarkRepository.count()).isEqualTo(15)
        assertThat(replayRepository.findAll()).allMatch {
            it.reporterId.matches(Regex("D3A[0-9A-F]{9}")) && it.sourceUrl.startsWith("https://replays.demo.invalid/")
        }
        assertThat(playerHardwareRepository.findAll()).allMatch {
            it.cpuMatchStatus == CpuMatchStatus.EXACT &&
                it.cpuScore != null &&
                it.replayCount in 3..4
        }
        assertThat(playerHardwareRepository.findAll()).anyMatch { it.aliasesJson == "[]" }
        assertThat(playerHardwareRepository.findAll()).anyMatch { it.aliasesJson != "[]" }
        assertThat(playerHardwareRepository.findAll().sumOf { it.replayCount }).isEqualTo(50)
        val atlas = playerHardwareRepository.findById("D3A000000001").orElseThrow()
        assertThat(atlas.mainName).isEqualTo("Atlas")
        assertThat(atlas.aliasesJson).contains("Atlas_GT", "Atlas2v2")
        assertThat(atlas.replayCount).isEqualTo(4)
        assertThat(cpuBenchmarkRepository.findAll()).allMatch {
            it.sourceUrl.startsWith("https://www.cpubenchmark.net/cpu.php?") &&
                it.sourceId.matches(Regex("\\d+"))
        }
        assertThat(REPLAY_SORT_PROPERTIES.keys).containsExactlyInAnyOrder(
            "matchAt", "reporter", "players", "mapName", "matchType", "duration", "cpu", "matchMode",
            "startCash", "gentoolVersion", "gameVersion", "windowsCompat", "repInfoInUse", "replaySize",
            "sourceDate", "collectedAt",
        )
        assertThat(CPU_PLAYER_SORT_PROPERTIES.keys).containsExactlyInAnyOrder(
            "mainName", "playerId", "aliases", "replayCount", "reportedCpu", "score", "latestName",
            "benchmark", "status", "observedAt", "scoreUpdatedAt",
        )
        val maps = replayRepository.findAll(PageRequest.of(0, 50, Sort.by("mapName"))).content.mapNotNull { it.mapName }
        assertThat(maps).isSorted
        val names = playerHardwareRepository.search(
            "",
            "all",
            PageRequest.of(0, 50, Sort.by("mainName")),
        ).content.map { it.mainName }
        assertThat(names).isSorted

        assertThat(contributors.sumOf { it.seed() }).isZero()
        assertThat(userRepository.count()).isEqualTo(2)
        assertThat(auditLogRepository.count()).isEqualTo(5)
        assertThat(replayRepository.count()).isEqualTo(50)
        assertThat(playerHardwareRepository.count()).isEqualTo(15)
    }
}
