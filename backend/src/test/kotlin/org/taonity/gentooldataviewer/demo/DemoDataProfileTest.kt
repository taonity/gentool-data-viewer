package org.taonity.gentooldataviewer.demo

import org.assertj.core.api.Assertions.assertThat
import org.taonity.gentooldataviewer.common.demo.DemoDataContributor
import org.taonity.gentooldataviewer.console.repository.AuditLogRepository
import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.user.repository.UserRepository
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.replay.repository.GentoolUserLinkRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayAssociatedFileRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayPlayerRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRepository
import org.taonity.gentooldataviewer.cpu.repository.CpuBenchmarkRepository
import org.taonity.gentooldataviewer.cpu.service.CpuMatchStatus
import org.taonity.gentooldataviewer.cpu.service.CPU_PLAYER_SORT_PROPERTIES
import org.taonity.gentooldataviewer.cpu.service.CpuPlayerQueryService
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
    private lateinit var gentoolUserLinkRepository: GentoolUserLinkRepository

    @Autowired
    private lateinit var cpuBenchmarkRepository: CpuBenchmarkRepository

    @Autowired
    private lateinit var cpuPlayerQueryService: CpuPlayerQueryService

    @Test
    fun `demo profile seeds feature data idempotently`() {
        val pending = userRepository.findByAccessStatusOrderByDisplayNameAsc(AccessRequestStatus.PENDING)
        assertThat(pending).hasSize(2)
        assertThat(pending.map { it.requestedRole }).containsExactly(ConsoleRole.VIEWER, ConsoleRole.EDITOR)

        assertThat(auditLogRepository.count()).isEqualTo(5)
        assertThat(auditLogRepository.existsByActorUserId("demo-data-owner")).isTrue()

        assertThat(replayRepository.count()).isEqualTo(50)
        assertThat(playerHardwareRepository.count()).isEqualTo(15)
        assertThat(replayPlayerRepository.count()).isEqualTo(166)
        assertThat(replayAssociatedFileRepository.count()).isEqualTo(100)
        assertThat(cpuBenchmarkRepository.count()).isEqualTo(15)
        assertThat(cpuPlayerQueryService.summary().dataSince)
            .isEqualTo(replayRepository.findAll().minOf { it.collectedAt })
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
        assertThat(gentoolUserLinkRepository.count()).isEqualTo(5)
        assertThat(cpuPlayerQueryService.list(null, null, 0, 50, null, null).content)
            .filteredOn { it.discordUser != null }
            .hasSize(5)
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
            "mainName", "discordUser", "playerId", "aliases", "replayCount", "reportedCpu", "score", "latestName",
            "benchmark", "status", "observedAt", "gentoolUpdatedAt", "scoreUpdatedAt",
        )
        val playersByDiscord = cpuPlayerQueryService.list(null, null, 0, 50, "discordUser", "asc").content
        assertThat(playersByDiscord.take(5).map { it.discordUser?.displayName }).isSorted
        assertThat(playersByDiscord.drop(5)).allMatch { it.discordUser == null }
        val playersByDiscordDescending =
            cpuPlayerQueryService.list(null, null, 0, 50, "discordUser", "desc").content
        assertThat(playersByDiscordDescending.take(5).mapNotNull { it.discordUser?.displayName })
            .isSortedAccordingTo(reverseOrder())
        assertThat(playersByDiscordDescending.drop(5)).allMatch { it.discordUser == null }
        val maps = replayRepository.findAll(PageRequest.of(0, 50, Sort.by("mapName"))).content.mapNotNull { it.mapName }
        assertThat(maps).isSorted
        val names = playerHardwareRepository.search(
            "",
            "all",
            PageRequest.of(0, 50, Sort.by("mainName")),
        ).content.map { it.mainName }
        assertThat(names).isSorted

        assertThat(contributors.sumOf { it.seed() }).isZero()
        assertThat(userRepository.count()).isEqualTo(7)
        assertThat(auditLogRepository.count()).isEqualTo(5)
        assertThat(replayRepository.count()).isEqualTo(50)
        assertThat(playerHardwareRepository.count()).isEqualTo(15)
        assertThat(gentoolUserLinkRepository.count()).isEqualTo(5)
    }
}
