package org.taonity.gentooldataviewer.cpu.service

import org.taonity.gentooldataviewer.cpu.entity.CpuBenchmarkEntity
import org.taonity.gentooldataviewer.cpu.repository.CpuBenchmarkRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class CpuBenchmarkCatalogService(
    private val repository: CpuBenchmarkRepository,
    private val matcher: CpuBenchmarkMatcher,
) {
    @Transactional
    fun replace(records: List<CpuBenchmarkRecord>, fetchedAt: Instant) {
        require(records.isNotEmpty()) { "Benchmark catalog was empty" }
        repository.deleteAllInBatch()
        repository.saveAll(
            records.map { record ->
                CpuBenchmarkEntity(
                    sourceId = record.sourceId,
                    modelName = record.modelName,
                    normalizedName = matcher.normalize(record.modelName),
                    normalizedModel = matcher.normalizeModel(record.modelName),
                    singleThreadScore = record.singleThreadScore,
                    sourceUrl = record.sourceUrl,
                    fetchedAt = fetchedAt,
                )
            },
        )
    }
}