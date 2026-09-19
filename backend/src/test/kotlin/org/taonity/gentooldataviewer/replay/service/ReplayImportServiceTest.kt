package org.taonity.gentooldataviewer.replay.service

import org.assertj.core.api.Assertions.assertThat
import org.taonity.gentooldataviewer.replay.parser.ParsedPlayer
import org.taonity.gentooldataviewer.replay.parser.ParsedReplay
import org.taonity.gentooldataviewer.replay.parser.ParsedAssociatedFile
import org.taonity.gentooldataviewer.replay.parser.ParsedTeam
import org.taonity.gentooldataviewer.replay.entity.PlayerHardwareEntity
import org.taonity.gentooldataviewer.replay.entity.ReplayEntity
import org.taonity.gentooldataviewer.replay.entity.GentoolLinkStatus
import org.taonity.gentooldataviewer.replay.entity.GentoolUserLinkEntity
import org.taonity.gentooldataviewer.cpu.entity.CpuBenchmarkEntity
import org.taonity.gentooldataviewer.cpu.repository.CpuBenchmarkRepository
import org.taonity.gentooldataviewer.cpu.service.CpuBenchmarkCatalogService
import org.taonity.gentooldataviewer.cpu.service.CpuBenchmarkMatcher
import org.taonity.gentooldataviewer.cpu.service.CpuBenchmarkRecord
import org.taonity.gentooldataviewer.cpu.service.CpuMatchStatus
import org.taonity.gentooldataviewer.cpu.service.CpuRatingService
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.replay.repository.GentoolUserLinkRepository
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
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.repository.UserRepository
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
    lateinit var linkRepository: GentoolUserLinkRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var benchmarkRepository: CpuBenchmarkRepository

    @Autowired
    lateinit var benchmarkMatcher: CpuBenchmarkMatcher

    @Autowired
    lateinit var catalogService: CpuBenchmarkCatalogService

    @Autowired
    lateinit var ratingService: CpuRatingService

    @BeforeEach
    fun prepare() = cleanUp()

    @AfterEach
    fun cleanUp() {
        linkRepository.deleteAll()
        replayPlayerRepository.deleteAll()
        replayAssociatedFileRepository.deleteAll()
        hardwareRepository.deleteAll()
        benchmarkRepository.deleteAll()
        replayRepository.deleteAll()
        userRepository.deleteAll()
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
        val importedReplay = replayRepository.findAll().single()
        assertThat(importedReplay.rawText).isEqualTo("GenTool Replay Information")
        mapOf(
            "matchAt" to "2026-09-02",
            "reporter" to "8313DCDFD572",
            "players" to "vit",
            "mapName" to "snow",
            "matchType" to "1v1",
            "duration" to "600",
            "cpu" to "13700K",
            "matchMode" to "LAN",
            "startCash" to "10000",
            "gentoolVersion" to "8.9",
            "gameVersion" to "Zero Hour",
            "windowsCompat" to "19045",
            "repInfoInUse" to "no",
            "replaySize" to "1000",
            "sourceDate" to "2026-09-02",
            "collectedAt" to importedReplay.collectedAt.toString().take(4),
            "all" to "LAN",
        ).forEach { (field, query) ->
            assertThat(replayRepository.search(query, field, PageRequest.of(0, 10)).totalElements)
                .describedAs("Replay search field %s", field)
                .isEqualTo(1)
        }
        assertThat(
            replayRepository.search(
                "", "all", PageRequest.of(0, 10), reporterIds = listOf("8313DCDFD572"), filterReporterIds = true,
            ).totalElements,
        ).isEqualTo(1)
        assertThat(
            replayRepository.search(
                "", "all", PageRequest.of(0, 10), reporterIds = listOf("OTHER_PLAYER"), filterReporterIds = true,
            ).totalElements,
        ).isZero()
        assertThat(
            replayRepository.search("", "all", PageRequest.of(0, 10), replayId = requireNotNull(importedReplay.id)).totalElements,
        ).isEqualTo(1)
        assertThat(
            replayRepository.search("", "all", PageRequest.of(0, 10), replayId = "00000000-0000-0000-0000-000000000000").totalElements,
        ).isZero()
        val hardware = hardwareRepository.findById("8313DCDFD572").orElseThrow()
        assertThat(hardware.latestName).isEqualTo("tao")
        assertThat(hardware.cpu).isEqualTo("13th Gen Intel Core i7-13700K")
        assertThat(hardware.cpuScore).isNull()
        assertThat(hardware.gentoolUpdatedAt).isNotNull()
        mapOf(
            "mainName" to "tao",
            "playerId" to "8313DCDFD572",
            "aliases" to "[]",
            "replayCount" to "1",
            "reportedCpu" to "13700K",
            "latestName" to "tao",
            "status" to "UNMATCHED",
            "gentoolUpdatedAt" to requireNotNull(hardware.gentoolUpdatedAt).toString().take(4),
            "all" to "13700K",
        ).forEach { (field, query) ->
            assertThat(hardwareRepository.search(query, field, PageRequest.of(0, 10)).totalElements)
                .describedAs("Player search field %s", field)
                .isEqualTo(1)
        }
        assertThat(hardwareRepository.search("", "all", PageRequest.of(0, 10), linkedOnly = true).totalElements)
            .isZero()
        val user = userRepository.save(
            UserEntity(
                googleId = "discord:100",
                authProvider = "discord",
                displayName = "Linked player",
            ),
        )
        linkRepository.save(
            GentoolUserLinkEntity(
                userId = user.userId,
                playerId = hardware.playerId,
                status = GentoolLinkStatus.APPROVED,
            ),
        )
        assertThat(hardwareRepository.search("", "all", PageRequest.of(0, 10), linkedOnly = true).totalElements)
            .isEqualTo(1)
        assertThat(
            hardwareRepository.search("", "all", PageRequest.of(0, 10), playerId = hardware.playerId).totalElements,
        ).isEqualTo(1)
        assertThat(
            hardwareRepository.search("", "all", PageRequest.of(0, 10), playerId = "OTHER_PLAYER").totalElements,
        ).isZero()
    }

    @Test
    fun `repairs players missing from previously imported no team replay`() {
        replayRepository.save(
            ReplayEntity(
                sourceUrl = "https://gentool.net/data/zh/no-team.txt",
                sourceDate = LocalDate.parse("2026-09-08"),
                reporterId = "8313DCDFD572",
                reporterName = "tao",
                playerNames = "",
                matchAt = Instant.parse("2026-09-08T20:16:25Z"),
                fieldsJson = "{}",
                rawText = NO_TEAM_RAW_TEXT,
            ),
        )

        assertThat(importService.repairMissingPlayers()).isEqualTo(1)
        assertThat(importService.repairMissingPlayers()).isZero()
        assertThat(replayRepository.findAll().single().playerNames).isEqualTo("tao, gla, cnc")
        assertThat(replayPlayerRepository.findAll())
            .extracting<String> { it.name }
            .containsExactly("tao", "gla", "cnc")
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
        mapOf(
            "score" to "4326",
            "benchmark" to "i7-13700K",
            "scoreUpdatedAt" to requireNotNull(rated.cpuScoreUpdatedAt).toString().take(4),
        ).forEach { (field, query) ->
            assertThat(hardwareRepository.search(query, field, PageRequest.of(0, 10)).totalElements)
                .describedAs("Player search field %s", field)
                .isEqualTo(1)
        }
        val sortedPlayerIds = hardwareRepository.search(
            "",
            "all",
            PageRequest.of(0, 100, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "cpuScore")),
        ).content
            .map { it.playerId }
        assertThat(sortedPlayerIds.indexOf("8313DCDFD572")).isLessThan(sortedPlayerIds.indexOf("NO_CPU"))
    }

    @Test
    fun `catalog replacement rebuilds old keys and rerates unmatched hardware`() {
        val namePairs = listOf(
            "AMD Ryzen 5 3500U w/ Radeon Vega Mobile Gfx" to "AMD Ryzen 5 3500U with Radeon Vega Mobile Gfx",
            "AMD Ryzen 5 1600 Six-Core Processor" to "AMD Ryzen 5 1600",
            "Intel(R) Core(TM)2 CPU E8400 @ 3.00GHz" to "Intel Core2 Duo E8400 @ 3.00GHz",
            "Intel(R) Core(TM) i7-2600 CPU @ 3.40GH" to "Intel Core i7-2600 @ 3.40GHz",
            "Intel(R) Core(TM) i5 CPU M 430 @ 2.27GHz" to "Intel Core i5-430M @ 2.27GHz",
            "Intel(R) Xeon(R) CPU E31220 @ 3.10GHz" to "Intel Xeon E3-1220 @ 3.10GHz",
            "Intel(R) Xeon(R) CPU E5-2670 0 @ 2.60GHz" to "Intel Xeon E5-2670 @ 2.60GHz",
        )
        val records = namePairs.mapIndexed { index, (_, name) ->
            CpuBenchmarkRecord("benchmark-$index", name, 1000 + index, "https://example/benchmark-$index")
        }
        val oldRecord = records.first()
        benchmarkRepository.save(
            CpuBenchmarkEntity(
                sourceId = oldRecord.sourceId,
                modelName = oldRecord.modelName,
                normalizedName = "amd ryzen 5 3500u with radeon vega mobile gfx",
                normalizedModel = "amd ryzen 5 3500u with radeon vega mobile gfx",
                singleThreadScore = oldRecord.singleThreadScore,
                sourceUrl = oldRecord.sourceUrl,
                fetchedAt = Instant.parse("2026-09-01T00:00:00Z"),
            ),
        )
        val rawNames = namePairs.map { it.first } + listOf("Intel(R) Core i7 processor", "AMD Ryzen AI 5 330 w/ Radeon 820M", null)
        rawNames.forEachIndexed { index, cpu ->
            importService.import(
                "https://gentool.net/data/zh/cpu-$index.txt",
                LocalDate.parse("2026-09-02"),
                replay().copy(reporterId = "CPU-$index", cpu = cpu),
            )
        }
        assertThat(hardwareRepository.findAll()).allSatisfy { assertThat(it.cpuScore).isNull() }

        val fetchedAt = Instant.parse("2026-09-03T00:00:00Z")
        catalogService.replace(
            records + listOf(
                CpuBenchmarkRecord("ai-330", "AMD Ryzen AI 5 330", 2000, "https://example/ai-330"),
                CpuBenchmarkRecord("ai-330-other", "AMD Ryzen AI 5 330 with Radeon 890M", 2100, "https://example/ai-330-other"),
            ),
            fetchedAt,
        )
        val summary = ratingService.rateAll()

        assertThat(summary.total).isEqualTo(rawNames.size)
        assertThat(summary.rated).isEqualTo(records.size)
        assertThat(summary.unmatched).isEqualTo(1)
        assertThat(summary.ambiguous).isEqualTo(1)
        assertThat(summary.noCpu).isEqualTo(1)
        records.forEachIndexed { index, record ->
            val hardware = hardwareRepository.findById("CPU-$index").orElseThrow()
            assertThat(hardware.cpu).isEqualTo(rawNames[index])
            assertThat(hardware.cpuScore).isEqualTo(record.singleThreadScore)
            assertThat(hardware.cpuBenchmarkId).isEqualTo(record.sourceId)
            assertThat(hardware.cpuBenchmarkName).isEqualTo(record.modelName)
            assertThat(hardware.cpuBenchmarkUrl).isEqualTo(record.sourceUrl)
            assertThat(hardware.cpuScoreUpdatedAt).isNotNull()
            assertThat(hardware.cpuMatchStatus).isIn(CpuMatchStatus.EXACT, CpuMatchStatus.MODEL)
        }
        val reindexed = benchmarkRepository.findById(oldRecord.sourceId).orElseThrow()
        assertThat(reindexed.normalizedName).isEqualTo("amd ryzen 5 3500u")
        assertThat(reindexed.normalizedModel).isEqualTo("amd ryzen 5 3500u")
        assertThat(reindexed.fetchedAt).isEqualTo(fetchedAt)
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

private val NO_TEAM_RAW_TEXT = """
    GenTool Replay Information

    Windows (Compat): 6.2.9200 SP 0.0
    System:           13th Gen Intel(R) Core(TM) i7-13700K

    GenTool Version:  8.9
    Player Name:      tao
    Player Id:        8313DCDFD572
    Match Date (UTC): 2026 Sep 08, 20:16:25
    Game Version:     Generals: Zero Hour
    Install Type:     Unknown
    RepInfo in use:   no

    Map Name:         maps/highlands
    Start Cash:       30000
    Match Type:       1v1v1
    Match Length:     00:31:59
    Match Mode:       LAN

    No Team
       1AB53000 tao (GLA)
       1AEE1000 gla (USA)
       1ACB9000 cnc (USA Superweapon)


    Associated files: 20-16-25_1v1v1_tao_gla_cnc.rep [246358 bytes]
""".trimIndent()