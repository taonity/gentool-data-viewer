package org.taonity.gentooldataviewer.cpu.dto

import org.taonity.gentooldataviewer.cpu.service.CpuMatchStatus
import org.taonity.gentooldataviewer.cpu.service.CpuBenchmarkSyncSummary
import org.taonity.gentooldataviewer.replay.entity.PlayerHardwareEntity
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper
import java.time.Instant

data class CpuPlayerDto(
    val playerId: String,
    val mainName: String,
    val latestName: String,
    val aliases: List<String>,
    val replayCount: Long,
    val reportedCpu: String?,
    val singleThreadScore: Int?,
    val benchmarkModel: String?,
    val benchmarkUrl: String?,
    val matchStatus: CpuMatchStatus,
    val observedAt: Instant,
    val gentoolUpdatedAt: Instant?,
    val scoreUpdatedAt: Instant?,
) {
    companion object {
        private val ALIASES_TYPE = object : TypeReference<List<String>>() {}

        fun from(entity: PlayerHardwareEntity, objectMapper: ObjectMapper) = CpuPlayerDto(
            playerId = entity.playerId,
            mainName = entity.mainName,
            latestName = entity.latestName,
            aliases = objectMapper.readValue(entity.aliasesJson, ALIASES_TYPE),
            replayCount = entity.replayCount,
            reportedCpu = entity.cpu,
            singleThreadScore = entity.cpuScore,
            benchmarkModel = entity.cpuBenchmarkName,
            benchmarkUrl = entity.cpuBenchmarkUrl,
            matchStatus = entity.cpuMatchStatus,
            observedAt = entity.observedAt,
            gentoolUpdatedAt = entity.gentoolUpdatedAt,
            scoreUpdatedAt = entity.cpuScoreUpdatedAt,
        )
    }
}

data class CpuPlayerSummaryDto(
    val totalPlayers: Long,
    val ratedPlayers: Long,
    val unmatchedPlayers: Long,
    val ambiguousPlayers: Long,
    val playersWithoutCpu: Long,
    val catalogEntries: Long,
    val catalogFetchedAt: Instant?,
    val dataSince: Instant?,
)

data class CpuBenchmarkSyncDto(
    val benchmarks: Int,
    val fetchedAt: Instant,
    val totalPlayers: Int,
    val ratedPlayers: Int,
    val unmatchedPlayers: Int,
    val ambiguousPlayers: Int,
    val playersWithoutCpu: Int,
) {
    companion object {
        fun from(summary: CpuBenchmarkSyncSummary) = CpuBenchmarkSyncDto(
            benchmarks = summary.benchmarks,
            fetchedAt = summary.fetchedAt,
            totalPlayers = summary.ratings.total,
            ratedPlayers = summary.ratings.rated,
            unmatchedPlayers = summary.ratings.unmatched,
            ambiguousPlayers = summary.ratings.ambiguous,
            playersWithoutCpu = summary.ratings.noCpu,
        )
    }
}