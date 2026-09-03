package org.example.fullstackstarter.replay.dto

import org.example.fullstackstarter.replay.entity.ReplayAssociatedFileEntity
import org.example.fullstackstarter.replay.entity.ReplayEntity
import org.example.fullstackstarter.replay.entity.ReplayPlayerEntity
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.time.LocalDate

data class ReplayPlayerDto(
    val teamNumber: Int,
    val slotNumber: Int,
    val address: String,
    val name: String,
    val army: String?,
) {
    companion object {
        fun from(entity: ReplayPlayerEntity) = ReplayPlayerDto(
            teamNumber = entity.teamNumber,
            slotNumber = entity.slotNumber,
            address = entity.address,
            name = entity.name,
            army = entity.army,
        )
    }
}

data class ReplayAssociatedFileDto(
    val name: String,
    val sizeBytes: Long,
) {
    companion object {
        fun from(entity: ReplayAssociatedFileEntity) = ReplayAssociatedFileDto(entity.fileName, entity.sizeBytes)
    }
}

data class ReplayDto(
    val id: String,
    val sourceUrl: String,
    val sourceDate: LocalDate,
    val reporterId: String,
    val reporterName: String,
    val matchAt: Instant,
    val windowsCompat: String?,
    val gentoolVersion: String?,
    val gameVersion: String?,
    val installType: String?,
    val repInfoInUse: String?,
    val mapName: String?,
    val startCash: Int?,
    val matchType: String?,
    val matchLengthSeconds: Long?,
    val matchMode: String?,
    val systemInfo: String?,
    val cpu: String?,
    val replayFileName: String?,
    val replaySizeBytes: Long?,
    val collectedAt: Instant,
    val fields: Map<String, String>,
    val players: List<ReplayPlayerDto>,
    val associatedFiles: List<ReplayAssociatedFileDto>,
    val rawText: String,
) {
    companion object {
        private val FIELDS_TYPE = object : TypeReference<Map<String, String>>() {}

        fun from(
            entity: ReplayEntity,
            players: List<ReplayPlayerEntity>,
            associatedFiles: List<ReplayAssociatedFileEntity>,
            objectMapper: ObjectMapper,
        ) = ReplayDto(
            id = requireNotNull(entity.id),
            sourceUrl = entity.sourceUrl,
            sourceDate = entity.sourceDate,
            reporterId = entity.reporterId,
            reporterName = entity.reporterName,
            matchAt = entity.matchAt,
            windowsCompat = entity.windowsCompat,
            gentoolVersion = entity.gentoolVersion,
            gameVersion = entity.gameVersion,
            installType = entity.installType,
            repInfoInUse = entity.repInfoInUse,
            mapName = entity.mapName,
            startCash = entity.startCash,
            matchType = entity.matchType,
            matchLengthSeconds = entity.matchLengthSeconds,
            matchMode = entity.matchMode,
            systemInfo = entity.systemInfo,
            cpu = entity.cpu,
            replayFileName = entity.replayFileName,
            replaySizeBytes = entity.replaySizeBytes,
            collectedAt = entity.collectedAt,
            fields = objectMapper.readValue(entity.fieldsJson, FIELDS_TYPE),
            players = players.sortedWith(compareBy(ReplayPlayerEntity::teamNumber, ReplayPlayerEntity::slotNumber))
                .map(ReplayPlayerDto::from),
            associatedFiles = associatedFiles.map(ReplayAssociatedFileDto::from),
            rawText = entity.rawText,
        )
    }
}