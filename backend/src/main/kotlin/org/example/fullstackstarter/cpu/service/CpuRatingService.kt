package org.example.fullstackstarter.cpu.service

import org.example.fullstackstarter.cpu.entity.CpuBenchmarkEntity
import org.example.fullstackstarter.cpu.repository.CpuBenchmarkRepository
import org.example.fullstackstarter.replay.entity.PlayerHardwareEntity
import org.example.fullstackstarter.replay.repository.PlayerHardwareRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CpuRatingService(
    private val benchmarkRepository: CpuBenchmarkRepository,
    private val hardwareRepository: PlayerHardwareRepository,
    private val matcher: CpuBenchmarkMatcher,
) {
    @Transactional
    fun rate(hardware: PlayerHardwareEntity) {
        apply(hardware, findMatch(hardware.cpu))
    }

    @Transactional
    fun rateAll(): RatingSummary {
        val hardware = hardwareRepository.findAll()
        hardware.forEach { apply(it, findMatch(it.cpu)) }
        hardwareRepository.saveAll(hardware)
        return RatingSummary(
            total = hardware.size,
            rated = hardware.count { it.cpuScore != null },
            unmatched = hardware.count { it.cpuMatchStatus == CpuMatchStatus.UNMATCHED },
            ambiguous = hardware.count { it.cpuMatchStatus == CpuMatchStatus.AMBIGUOUS },
            noCpu = hardware.count { it.cpuMatchStatus == CpuMatchStatus.NO_CPU },
        )
    }

    private fun findMatch(cpu: String?): CpuMatch {
        if (cpu.isNullOrBlank()) return CpuMatch(CpuMatchStatus.NO_CPU)
        val exact = benchmarkRepository.findByNormalizedName(matcher.normalize(cpu)).map { it.toRecord() }
        if (exact.isNotEmpty()) return matcher.match(cpu, exact)
        val model = benchmarkRepository.findByNormalizedModel(matcher.normalizeModel(cpu)).map { it.toRecord() }
        return matcher.match(cpu, model)
    }

    private fun apply(hardware: PlayerHardwareEntity, match: CpuMatch) {
        val benchmark = match.benchmark
        hardware.cpuMatchStatus = match.status
        hardware.cpuScore = benchmark?.singleThreadScore
        hardware.cpuBenchmarkId = benchmark?.sourceId
        hardware.cpuBenchmarkName = benchmark?.modelName
        hardware.cpuBenchmarkUrl = benchmark?.sourceUrl
        hardware.cpuScoreUpdatedAt = if (benchmark == null) null else Instant.now()
    }

    private fun CpuBenchmarkEntity.toRecord() =
        CpuBenchmarkRecord(sourceId, modelName, singleThreadScore, sourceUrl)
}

data class RatingSummary(
    val total: Int,
    val rated: Int,
    val unmatched: Int,
    val ambiguous: Int,
    val noCpu: Int,
)