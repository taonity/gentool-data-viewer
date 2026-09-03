package org.example.fullstackstarter.cpu.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "cpu_benchmark")
class CpuBenchmarkEntity(
    @Id
    @Column(name = "source_id")
    val sourceId: String,
    @Column(name = "model_name", nullable = false, length = 500)
    val modelName: String,
    @Column(name = "normalized_name", nullable = false, length = 500)
    val normalizedName: String,
    @Column(name = "normalized_model", nullable = false, length = 500)
    val normalizedModel: String,
    @Column(name = "single_thread_score", nullable = false)
    val singleThreadScore: Int,
    @Column(name = "source_url", nullable = false, length = 1000)
    val sourceUrl: String,
    @Column(name = "fetched_at", nullable = false)
    val fetchedAt: Instant,
)