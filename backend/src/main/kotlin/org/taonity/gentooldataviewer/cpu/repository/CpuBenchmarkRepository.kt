package org.taonity.gentooldataviewer.cpu.repository

import org.taonity.gentooldataviewer.cpu.entity.CpuBenchmarkEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CpuBenchmarkRepository : JpaRepository<CpuBenchmarkEntity, String> {
    fun findByNormalizedName(normalizedName: String): List<CpuBenchmarkEntity>

    fun findByNormalizedModel(normalizedModel: String): List<CpuBenchmarkEntity>

    fun findTopByOrderByFetchedAtDesc(): CpuBenchmarkEntity?

}