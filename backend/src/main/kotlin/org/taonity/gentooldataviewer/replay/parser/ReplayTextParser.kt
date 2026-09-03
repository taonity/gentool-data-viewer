package org.taonity.gentooldataviewer.replay.parser

import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

data class ParsedReplay(
    val rawText: String,
    val fields: Map<String, String>,
    val windowsCompat: String?,
    val gentoolVersion: String?,
    val reporterName: String,
    val reporterId: String,
    val matchDate: Instant,
    val gameVersion: String?,
    val installType: String?,
    val repInfoInUse: String?,
    val mapName: String?,
    val startCash: Int?,
    val matchType: String?,
    val matchLength: Duration?,
    val matchMode: String?,
    val system: String?,
    val cpu: String?,
    val teams: List<ParsedTeam>,
    val associatedFiles: List<ParsedAssociatedFile>,
    val replayFileName: String?,
    val replaySizeBytes: Long?,
)

data class ParsedTeam(
    val number: Int,
    val players: List<ParsedPlayer>,
)

data class ParsedPlayer(
    val address: String,
    val name: String,
    val army: String?,
)

data class ParsedAssociatedFile(
    val name: String,
    val sizeBytes: Long,
)

@Component
class ReplayTextParser {
    fun parse(text: String): ParsedReplay {
        val normalizedText = text.replace("\r\n", "\n").replace('\r', '\n').trimEnd()
        val lines = normalizedText.lines()
        val fields = parseFields(lines)
        val systemLines = block(lines, "System:")
        val associatedFiles = block(lines, "Associated files:")
            .mapNotNull(::parseAssociatedFile)
        val replayFile = associatedFiles
            .firstOrNull { it.first.endsWith(".rep", ignoreCase = true) }

        return ParsedReplay(
            rawText = normalizedText,
            fields = fields,
            windowsCompat = fields["Windows (Compat)"],
            gentoolVersion = value(lines, "GenTool Version:"),
            reporterName = requiredValue(lines, "Player Name:"),
            reporterId = requiredValue(lines, "Player Id:"),
            matchDate = LocalDateTime.parse(requiredValue(lines, "Match Date (UTC):"), MATCH_DATE_FORMAT)
                .toInstant(ZoneOffset.UTC),
            gameVersion = value(lines, "Game Version:"),
            installType = value(lines, "Install Type:"),
            repInfoInUse = value(lines, "RepInfo in use:"),
            mapName = value(lines, "Map Name:"),
            startCash = value(lines, "Start Cash:")?.toIntOrNull(),
            matchType = value(lines, "Match Type:"),
            matchLength = value(lines, "Match Length:")?.let(::parseDuration),
            matchMode = value(lines, "Match Mode:"),
            system = systemLines.joinToString("\n").ifBlank { null },
            cpu = systemLines.firstOrNull(CPU_PATTERN::containsMatchIn),
            teams = parseTeams(lines),
            associatedFiles = associatedFiles.map { ParsedAssociatedFile(it.first, it.second) },
            replayFileName = replayFile?.first,
            replaySizeBytes = replayFile?.second,
        )
    }

    private fun parseFields(lines: List<String>): Map<String, String> = buildMap {
        lines.forEachIndexed { index, line ->
            val match = FIELD_PATTERN.matchEntire(line) ?: return@forEachIndexed
            val label = match.groupValues[1].trim()
            val values = mutableListOf<String>()
            match.groupValues[2].trim().takeIf(String::isNotBlank)?.let(values::add)
            for (continuation in lines.drop(index + 1)) {
                if (continuation.isBlank() || !continuation.startsWith(' ')) break
                values += continuation.trim()
            }
            put(label, values.joinToString("\n"))
        }
    }

    private fun value(lines: List<String>, label: String): String? =
        lines.firstOrNull { it.startsWith(label) }
            ?.substring(label.length)
            ?.trim()
            ?.ifBlank { null }

    private fun requiredValue(lines: List<String>, label: String): String =
        value(lines, label) ?: throw ReplayParseException("Missing required field '$label'")

    private fun block(lines: List<String>, label: String): List<String> {
        val start = lines.indexOfFirst { it.startsWith(label) }
        if (start < 0) return emptyList()

        val values = mutableListOf<String>()
        lines[start].substring(label.length).trim().takeIf(String::isNotBlank)?.let(values::add)
        for (line in lines.drop(start + 1)) {
            if (line.isBlank() || !line.startsWith(' ')) break
            values += line.trim()
        }
        return values
    }

    private fun parseTeams(lines: List<String>): List<ParsedTeam> {
        val teams = mutableListOf<ParsedTeam>()
        var index = 0
        while (index < lines.size) {
            val teamMatch = TEAM_PATTERN.matchEntire(lines[index].trim())
            if (teamMatch == null) {
                index++
                continue
            }

            val players = mutableListOf<ParsedPlayer>()
            index++
            while (index < lines.size) {
                val playerMatch = PLAYER_PATTERN.matchEntire(lines[index]) ?: break
                players += ParsedPlayer(
                    address = playerMatch.groupValues[1],
                    name = playerMatch.groupValues[2].trim(),
                    army = playerMatch.groupValues[3].ifBlank { null },
                )
                index++
            }
            teams += ParsedTeam(teamMatch.groupValues[1].toInt(), players)
        }
        return teams
    }

    private fun parseDuration(value: String): Duration {
        val parts = value.split(':').map(String::toLong)
        if (parts.size != 3) throw ReplayParseException("Invalid match length '$value'")
        return Duration.ofHours(parts[0]).plusMinutes(parts[1]).plusSeconds(parts[2])
    }

    private fun parseAssociatedFile(value: String): Pair<String, Long>? {
        val match = ASSOCIATED_FILE_PATTERN.matchEntire(value) ?: return null
        return match.groupValues[1] to match.groupValues[2].toLong()
    }

    private companion object {
        val MATCH_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy MMM dd, HH:mm:ss", Locale.ENGLISH)
        val TEAM_PATTERN = Regex("Team (\\d+)")
        val PLAYER_PATTERN = Regex("\\s+([0-9A-Fa-f]+)\\s+(.+?)(?:\\s+\\(([^()]*)\\))?\\s*")
        val FIELD_PATTERN = Regex("([^:]+):\\s*(.*)")
        val ASSOCIATED_FILE_PATTERN = Regex("(.+) \\[(\\d+) bytes]")
        val CPU_PATTERN = Regex(
            "(?i)\\b(?:Intel|AMD)\\b.*\\b(?:Core|Ryzen|Xeon|Pentium|Celeron|Athlon|Threadripper)\\b|" +
                "\\b(?:Core|Ryzen|Xeon|Pentium|Celeron|Athlon|Threadripper)\\b.*\\b(?:Intel|AMD)\\b",
        )
    }
}

class ReplayParseException(message: String) : RuntimeException(message)