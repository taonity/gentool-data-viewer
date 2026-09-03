package org.taonity.gentooldataviewer.replay.service

import org.taonity.gentooldataviewer.replay.entity.PlayerHardwareEntity
import org.taonity.gentooldataviewer.replay.entity.ReplayAssociatedFileEntity
import org.taonity.gentooldataviewer.replay.entity.ReplayEntity
import org.taonity.gentooldataviewer.replay.entity.ReplayPlayerEntity
import org.taonity.gentooldataviewer.replay.parser.ParsedReplay
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayAssociatedFileRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayPlayerRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRepository
import org.taonity.gentooldataviewer.cpu.service.CpuRatingService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

@Service
class ReplayImportService(
    private val replayRepository: ReplayRepository,
    private val replayPlayerRepository: ReplayPlayerRepository,
    private val replayAssociatedFileRepository: ReplayAssociatedFileRepository,
    private val playerHardwareRepository: PlayerHardwareRepository,
    private val cpuRatingService: CpuRatingService,
    private val objectMapper: ObjectMapper,
) {
    fun exists(sourceUrl: String): Boolean = replayRepository.existsBySourceUrl(sourceUrl)

    @Transactional
    fun import(sourceUrl: String, sourceDate: LocalDate, parsed: ParsedReplay): Boolean {
        if (replayRepository.existsBySourceUrl(sourceUrl)) return false

        val replay = replayRepository.save(
            ReplayEntity(
                sourceUrl = sourceUrl,
                sourceDate = sourceDate,
                reporterId = parsed.reporterId,
                reporterName = parsed.reporterName,
                playerNames = parsed.teams.flatMap { it.players }.joinToString(", ") { it.name },
                matchAt = parsed.matchDate,
                windowsCompat = parsed.windowsCompat,
                gentoolVersion = parsed.gentoolVersion,
                gameVersion = parsed.gameVersion,
                installType = parsed.installType,
                repInfoInUse = parsed.repInfoInUse,
                mapName = parsed.mapName,
                startCash = parsed.startCash,
                matchType = parsed.matchType,
                matchLengthSeconds = parsed.matchLength?.seconds,
                matchMode = parsed.matchMode,
                systemInfo = parsed.system,
                cpu = parsed.cpu,
                replayFileName = parsed.replayFileName,
                replaySizeBytes = parsed.replaySizeBytes,
                fieldsJson = objectMapper.writeValueAsString(parsed.fields),
                rawText = parsed.rawText,
            ),
        )
        val replayId = requireNotNull(replay.id)
        replayPlayerRepository.saveAll(
            parsed.teams.flatMap { team ->
                team.players.mapIndexed { slot, player ->
                    ReplayPlayerEntity(
                        replayId = replayId,
                        teamNumber = team.number,
                        slotNumber = slot + 1,
                        address = player.address,
                        name = player.name,
                        army = player.army,
                    )
                }
            },
        )
        replayAssociatedFileRepository.saveAll(
            parsed.associatedFiles.map { file ->
                ReplayAssociatedFileEntity(
                    replayId = replayId,
                    fileName = file.name,
                    sizeBytes = file.sizeBytes,
                )
            },
        )
        updateHardware(replayId, parsed)
        return true
    }

    private fun updateHardware(replayId: String, parsed: ParsedReplay) {
        val current = playerHardwareRepository.findById(parsed.reporterId).orElse(null)
        if (current == null) {
            val hardware = PlayerHardwareEntity(
                    playerId = parsed.reporterId,
                    latestName = parsed.reporterName,
                    cpu = parsed.cpu,
                    systemInfo = parsed.system,
                    observedAt = parsed.matchDate,
                    sourceReplayId = replayId,
                )
            val managed = playerHardwareRepository.save(hardware)
            updateIdentitySummary(managed)
            cpuRatingService.rate(managed)
        } else if (parsed.matchDate >= current.observedAt) {
            current.latestName = parsed.reporterName
            current.cpu = parsed.cpu
            current.systemInfo = parsed.system
            current.observedAt = parsed.matchDate
            current.sourceReplayId = replayId
            updateIdentitySummary(current)
            cpuRatingService.rate(current)
        } else {
            updateIdentitySummary(current)
        }
    }

    private fun updateIdentitySummary(hardware: PlayerHardwareEntity) {
        val replays = replayRepository.findByReporterId(hardware.playerId)
        val namesByFrequency = replays
            .groupBy(ReplayEntity::reporterName)
            .map { (name, occurrences) ->
                NameFrequency(name, occurrences.size, occurrences.maxOf(ReplayEntity::matchAt))
            }
            .sortedWith(
                compareByDescending<NameFrequency> { it.count }
                    .thenByDescending { it.latestAt }
                    .thenBy { it.name },
            )
        val mainName = namesByFrequency.firstOrNull()?.name ?: hardware.latestName
        hardware.mainName = mainName
        hardware.aliasesJson = objectMapper.writeValueAsString(namesByFrequency.map { it.name }.filterNot { it == mainName })
        hardware.replayCount = replays.size.toLong()
    }

    private data class NameFrequency(
        val name: String,
        val count: Int,
        val latestAt: java.time.Instant,
    )
}