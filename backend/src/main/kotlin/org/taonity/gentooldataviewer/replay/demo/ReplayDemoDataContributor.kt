package org.taonity.gentooldataviewer.replay.demo

import org.taonity.gentooldataviewer.common.demo.DemoDataContributor
import org.taonity.gentooldataviewer.cpu.entity.CpuBenchmarkEntity
import org.taonity.gentooldataviewer.cpu.repository.CpuBenchmarkRepository
import org.taonity.gentooldataviewer.cpu.service.CpuBenchmarkMatcher
import org.taonity.gentooldataviewer.replay.parser.ParsedAssociatedFile
import org.taonity.gentooldataviewer.replay.parser.ParsedPlayer
import org.taonity.gentooldataviewer.replay.parser.ParsedReplay
import org.taonity.gentooldataviewer.replay.parser.ParsedTeam
import org.taonity.gentooldataviewer.replay.service.ReplayImportService
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Component
@Profile("demo-data")
@Order(300)
class ReplayDemoDataContributor(
    private val replayImportService: ReplayImportService,
    private val benchmarkRepository: CpuBenchmarkRepository,
    private val benchmarkMatcher: CpuBenchmarkMatcher,
) : DemoDataContributor {
    override val feature: String = "replays"

    @Transactional
    override fun seed(): Int {
        if (replayImportService.exists(sourceUrl(0))) return 0

        val fetchedAt = Instant.parse("2026-09-01T00:00:00Z")
        benchmarkRepository.saveAll(
            PLAYERS.map { player ->
                CpuBenchmarkEntity(
                    sourceId = player.benchmarkId,
                    modelName = player.cpu,
                    normalizedName = benchmarkMatcher.normalize(player.cpu),
                    normalizedModel = benchmarkMatcher.normalizeModel(player.cpu),
                    singleThreadScore = player.score,
                    sourceUrl = passMarkUrl(player),
                    fetchedAt = fetchedAt,
                )
            },
        )

        val imported = (0 until REPLAY_COUNT).count { index ->
            val reporter = PLAYERS[index % PLAYERS.size]
            val reporterName = reporter.nameForOccurrence(index / PLAYERS.size)
            replayImportService.import(
                sourceUrl(index),
                DEMO_DATE.minusDays((index / 10).toLong()),
                replay(index, reporter, reporterName),
            )
        }
        check(imported == REPLAY_COUNT) { "Expected $REPLAY_COUNT demo replays, imported $imported" }
        return imported + PLAYERS.size
    }

    private fun replay(index: Int, reporter: DemoPlayer, reporterName: String): ParsedReplay {
        val participants = (0 until 4).map { offset -> PLAYERS[(index + offset) % PLAYERS.size] }
        val participantNames = participants.mapIndexed { offset, player ->
            if (offset == 0) reporterName else player.nameForReplay(index + offset)
        }
        val matchAt = DEMO_START.minusSeconds(index * 2_137L)
        val map = MAPS[index % MAPS.size]
        val matchType = if (index % 3 == 0) "1v1" else "2v2"
        val duration = Duration.ofMinutes((8 + index % 34).toLong()).plusSeconds((index * 17 % 60).toLong())
        val replayFile = "demo-${index + 1}-${matchType}.rep"
        val reportedCpu = reporter.cpu.takeIf { reporter.reportsCpu }
        val systemInfo = listOfNotNull(
            "Demo Mainboard D${index % 5 + 1}",
            reportedCpu,
            "Demo Graphics Adapter",
        ).joinToString("\n")
        val fields = linkedMapOf(
            "Windows (Compat)" to "10.0.19045 SP 0.0",
            "System" to systemInfo,
            "GenTool Version" to "8.9",
            "Player Name" to reporterName,
            "Player Id" to reporter.id,
            "Match Date (UTC)" to matchAt.toString(),
            "Game Version" to "Zero Hour 1.04 Demo Install",
            "Install Type" to "Synthetic Demo Install",
            "RepInfo in use" to if (index % 4 == 0) "yes" else "no",
            "Map Name" to map,
            "Start Cash" to if (index % 5 == 0) "50000" else "10000",
            "Match Type" to matchType,
            "Match Length" to duration.toString(),
            "Match Mode" to MODES[index % MODES.size],
        )
        val teams = if (matchType == "1v1") {
            listOf(
                ParsedTeam(1, listOf(participant(participants[0], participantNames[0], index, 0))),
                ParsedTeam(2, listOf(participant(participants[1], participantNames[1], index, 1))),
            )
        } else {
            listOf(
                ParsedTeam(1, participants.take(2).mapIndexed { slot, player ->
                    participant(player, participantNames[slot], index, slot)
                }),
                ParsedTeam(2, participants.drop(2).mapIndexed { slot, player ->
                    participant(player, participantNames[slot + 2], index, slot + 2)
                }),
            )
        }
        val associatedFiles = listOf(
            ParsedAssociatedFile(replayFile, 90_000L + index * 4_321L),
            ParsedAssociatedFile("demo-${index + 1}-shot1.jpg", 48_000L + index * 317L),
        )

        return ParsedReplay(
            rawText = rawText(fields, teams, associatedFiles),
            fields = fields,
            windowsCompat = fields["Windows (Compat)"],
            gentoolVersion = fields["GenTool Version"],
            reporterName = reporterName,
            reporterId = reporter.id,
            matchDate = matchAt,
            gameVersion = fields["Game Version"],
            installType = fields["Install Type"],
            repInfoInUse = fields["RepInfo in use"],
            mapName = map,
            startCash = fields["Start Cash"]?.toInt(),
            matchType = matchType,
            matchLength = duration,
            matchMode = fields["Match Mode"],
            system = systemInfo,
            cpu = reportedCpu,
            teams = teams,
            associatedFiles = associatedFiles,
            replayFileName = replayFile,
            replaySizeBytes = associatedFiles.first().sizeBytes,
        )
    }

    private fun participant(player: DemoPlayer, name: String, replayIndex: Int, slot: Int) = ParsedPlayer(
        address = "%08X".format(0x10000000 + replayIndex * 16 + slot),
        name = name,
        army = ARMIES[(replayIndex + slot) % ARMIES.size],
    )

    private fun rawText(
        fields: Map<String, String>,
        teams: List<ParsedTeam>,
        files: List<ParsedAssociatedFile>,
    ): String = buildString {
        appendLine("GenTool Replay Information (synthetic demo fixture)")
        appendLine()
        fields.forEach { (label, value) -> appendLine("$label: $value") }
        appendLine()
        teams.forEach { team ->
            appendLine("Team ${team.number}")
            team.players.forEach { player -> appendLine("   ${player.address} ${player.name} (${player.army})") }
        }
        appendLine()
        files.forEachIndexed { index, file ->
            appendLine("${if (index == 0) "Associated files: " else "                  "}${file.name} [${file.sizeBytes} bytes]")
        }
    }.trimEnd()

    private fun sourceUrl(index: Int): String = "https://replays.demo.invalid/zh/demo-${index + 1}.txt"

    private fun passMarkUrl(player: DemoPlayer): String =
        "https://www.cpubenchmark.net/cpu.php?cpu=${URLEncoder.encode(player.cpu, StandardCharsets.UTF_8)}&id=${player.benchmarkId}"

    private data class DemoPlayer(
        val id: String,
        val mainName: String,
        val aliases: List<String>,
        val cpu: String,
        val score: Int,
        val benchmarkId: String,
        val reportsCpu: Boolean = true,
    ) {
        fun nameForOccurrence(occurrence: Int): String =
            if (occurrence < 2 || aliases.isEmpty()) mainName else aliases[(occurrence - 2) % aliases.size]

        fun nameForReplay(seed: Int): String =
            if (aliases.isNotEmpty() && seed % 4 == 0) aliases[seed % aliases.size] else mainName
    }

    private companion object {
        const val REPLAY_COUNT = 50
        val DEMO_DATE: LocalDate = LocalDate.parse("2026-09-02")
        val DEMO_START: Instant = Instant.parse("2026-09-02T22:45:00Z")
        val PLAYERS = listOf(
            DemoPlayer("D3A000000001", "Atlas", listOf("Atlas_GT", "Atlas2v2"), "Intel Core i7-13700K", 4326, "5060"),
            DemoPlayer("D3A000000002", "Birch", listOf("BirchLeaf", "B1rch"), "AMD Ryzen 7 7800X3D", 3759, "5299"),
            DemoPlayer("D3A000000003", "Cobalt", listOf("CobaltZH", "CoBalt"), "Intel Core i5-12600K", 3917, "4603"),
            DemoPlayer("D3A000000004", "Delta", listOf("DeltaOne", "D3lta"), "AMD Ryzen 5 5600X", 3366, "3859"),
            DemoPlayer("D3A000000005", "Ember", listOf("EmberFox", "3mber"), "Intel Core i9-12900K", 4128, "4597"),
            DemoPlayer("D3A000000006", "Flux", listOf("FluxCap", "Fluxx"), "AMD Ryzen 9 5900X", 3465, "3870"),
            DemoPlayer("D3A000000007", "Grove", emptyList(), "AMD Ryzen 7 5800X3D", 3233, "4823", reportsCpu = false),
            DemoPlayer("D3A000000008", "Halo", listOf("HaloRush", "HaLo"), "Intel Core i5-13400F", 3628, "5166"),
            DemoPlayer("D3A000000009", "Ion", listOf("IonStorm", "I0n"), "AMD Ryzen 5 7600", 3907, "5172"),
            DemoPlayer("D3A00000000A", "Jade", listOf("Jadeite", "J4de"), "Intel Core i7-12700K", 4003, "4609"),
            DemoPlayer("D3A00000000B", "Kilo", emptyList(), "Intel Core i5-12400F", 3485, "4681"),
            DemoPlayer("D3A00000000C", "Lumen", listOf("LumenZH", "Lum3n"), "AMD Ryzen 7 7700", 4050, "5169"),
            DemoPlayer("D3A00000000D", "Mesa", listOf("MesaHigh", "M3sa"), "Intel Core i3-12100", 3242, "4687"),
            DemoPlayer("D3A00000000E", "Nova", emptyList(), "AMD Ryzen 5 5600", 3253, "4811", reportsCpu = false),
            DemoPlayer("D3A00000000F", "Orbit", listOf("OrbitZH", "0rbit"), "Intel Core i7-13700F", 4117, "5163"),
        )
        val MAPS = listOf("Demo Alpine Pass", "Demo Desert Basin", "Demo River Crossing", "Demo Tournament Field")
        val MODES = listOf("LAN", "Online", "Hamachi")
        val ARMIES = listOf("USA", "China", "GLA", "Random")
    }
}