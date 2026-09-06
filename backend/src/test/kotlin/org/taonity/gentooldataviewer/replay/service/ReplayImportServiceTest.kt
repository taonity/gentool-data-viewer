package org.taonity.gentooldataviewer.replay.service

import org.assertj.core.api.Assertions.assertThat
import org.taonity.gentooldataviewer.replay.parser.ParsedPlayer
import org.taonity.gentooldataviewer.replay.parser.ParsedReplay
import org.taonity.gentooldataviewer.replay.parser.ParsedAssociatedFile
import org.taonity.gentooldataviewer.replay.parser.ParsedTeam
import org.taonity.gentooldataviewer.replay.entity.PlayerHardwareEntity
import org.taonity.gentooldataviewer.cpu.entity.CpuBenchmarkEntity
import org.taonity.gentooldataviewer.cpu.repository.CpuBenchmarkRepository
import org.taonity.gentooldataviewer.cpu.service.CpuBenchmarkMatcher
import org.taonity.gentooldataviewer.cpu.service.CpuMatchStatus
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayAssociatedFileRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayPlayerRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("h2")
class ReplayImportServiceTest {
    @Autowired
    lateinit var importService: ReplayImportService

    @Autowired
    lateinit var replayRepository: ReplayRepository

    @Autowired
    lateinit var replayPlayerRepository: ReplayPlayerRepository

    @Autowired
    lateinit var replayAssociatedFileRepository: ReplayAssociatedFileRepository

    @Autowired
    lateinit var hardwareRepository: PlayerHardwareRepository

    @Autowired
    lateinit var benchmarkRepository: CpuBenchmarkRepository

    @Autowired
    lateinit var benchmarkMatcher: CpuBenchmarkMatcher

    @BeforeEach
    fun prepare() = cleanUp()

    @AfterEach
    fun cleanUp() {
        replayPlayerRepository.deleteAll()
        replayAssociatedFileRepository.deleteAll()
        hardwareRepository.deleteAll()
        benchmarkRepository.deleteAll()
        replayRepository.deleteAll()
    }

    @Test
    fun `imports a replay once with players and latest reporter hardware`() {
        val sourceUrl = "https://gentool.net/data/zh/sample.txt"

        assertThat(importService.import(sourceUrl, LocalDate.parse("2026-09-02"), replay())).isTrue()
        assertThat(importService.import(sourceUrl, LocalDate.parse("2026-09-02"), replay())).isFalse()

        assertThat(replayRepository.count()).isEqualTo(1)
        assertThat(replayPlayerRepository.findAll()).extracting<String> { it.name }
            .containsExactlyInAnyOrder("tao", "vit")
        assertThat(replayAssociatedFileRepository.findAll()).extracting<String> { it.fileName }
            .containsExactly("sample.rep")
        assertThat(replayRepository.findAll().single().rawText).isEqualTo("GenTool Replay Information")
        assertThat(replayRepository.search("vit", "player", PageRequest.of(0, 10)).totalElements).isEqualTo(1)
        val hardware = hardwareRepository.findById("8313DCDFD572").orElseThrow()
        assertThat(hardware.latestName).isEqualTo("tao")
        assertThat(hardware.cpu).isEqualTo("13th Gen Intel Core i7-13700K")
        assertThat(hardware.cpuScore).isNull()
        assertThat(hardware.gentoolUpdatedAt).isNotNull()
    }

    @Test
    fun `rates imported hardware and sorts unresolved scores last`() {
        val benchmarkName = "Intel Core i7-13700K"
        benchmarkRepository.save(
            CpuBenchmarkEntity(
                sourceId = "5060",
                modelName = benchmarkName,
                normalizedName = benchmarkMatcher.normalize(benchmarkName),
                normalizedModel = benchmarkMatcher.normalizeModel(benchmarkName),
                singleThreadScore = 4326,
                sourceUrl = "https://www.cpubenchmark.net/cpu.php?id=5060",
                fetchedAt = Instant.parse("2026-09-03T00:00:00Z"),
            ),
        )
        importService.import("https://gentool.net/data/zh/rated.txt", LocalDate.parse("2026-09-02"), replay())
        val replayId = requireNotNull(
            replayRepository.findAll().single { it.sourceUrl == "https://gentool.net/data/zh/rated.txt" }.id,
        )
        hardwareRepository.save(
            PlayerHardwareEntity(
                playerId = "NO_CPU",
                latestName = "Unrated",
                observedAt = Instant.parse("2026-09-02T18:00:00Z"),
                sourceReplayId = replayId,
            ),
        )

        val rated = hardwareRepository.findById("8313DCDFD572").orElseThrow()
        assertThat(rated.cpuScore).isEqualTo(4326)
        assertThat(rated.cpuBenchmarkName).isEqualTo(benchmarkName)
        assertThat(rated.cpuMatchStatus).isEqualTo(CpuMatchStatus.EXACT)
        val sortedPlayerIds = hardwareRepository.search(
            "",
            "all",
            PageRequest.of(0, 100, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "cpuScore")),
        ).content
            .map { it.playerId }
        assertThat(sortedPlayerIds.indexOf("8313DCDFD572")).isLessThan(sortedPlayerIds.indexOf("NO_CPU"))
    }

    private fun replay() = ParsedReplay(
        rawText = "GenTool Replay Information",
        fields = mapOf("GenTool Version" to "8.9"),
        windowsCompat = "10.0.19045 SP 0.0",
        gentoolVersion = "8.9",
        reporterName = "tao",
        reporterId = "8313DCDFD572",
        matchDate = Instant.parse("2026-09-02T17:59:26Z"),
        gameVersion = "Zero Hour 1.04",
        installType = "Modified Game Install",
        repInfoInUse = "no",
        mapName = "maps/keep of the snow",
        startCash = 10000,
        matchType = "1v1",
        matchLength = Duration.ofMinutes(10),
        matchMode = "LAN",
        system = "13th Gen Intel Core i7-13700K",
        cpu = "13th Gen Intel Core i7-13700K",
        teams = listOf(
            ParsedTeam(1, listOf(ParsedPlayer("1A", "tao", "Random"))),
            ParsedTeam(2, listOf(ParsedPlayer("1B", "vit", "USA"))),
        ),
        associatedFiles = listOf(ParsedAssociatedFile("sample.rep", 1000)),
        replayFileName = "sample.rep",
        replaySizeBytes = 1000,
    )
}