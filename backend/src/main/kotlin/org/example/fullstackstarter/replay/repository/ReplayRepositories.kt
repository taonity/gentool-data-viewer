package org.example.fullstackstarter.replay.repository

import org.example.fullstackstarter.replay.entity.PlayerHardwareEntity
import org.example.fullstackstarter.replay.entity.ReplayCollectionJobEntity
import org.example.fullstackstarter.replay.entity.ReplayAssociatedFileEntity
import org.example.fullstackstarter.replay.entity.ReplayEntity
import org.example.fullstackstarter.replay.entity.ReplayPlayerEntity
import org.example.fullstackstarter.cpu.service.CpuMatchStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface ReplayRepository : JpaRepository<ReplayEntity, String> {
    fun existsBySourceUrl(sourceUrl: String): Boolean

    fun findByReporterId(reporterId: String): List<ReplayEntity>

    @Query(
        """
        SELECT DISTINCT r FROM ReplayEntity r
        LEFT JOIN ReplayPlayerEntity p ON p.replayId = r.id
        WHERE (:field = 'all' AND LOWER(r.rawText) LIKE LOWER(CONCAT('%', :q, '%')))
           OR (:field = 'reporterName' AND LOWER(r.reporterName) LIKE LOWER(CONCAT('%', :q, '%')))
           OR (:field = 'reporterId' AND LOWER(r.reporterId) LIKE LOWER(CONCAT('%', :q, '%')))
           OR (:field = 'player' AND LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')))
           OR (:field = 'mapName' AND LOWER(COALESCE(r.mapName, '')) LIKE LOWER(CONCAT('%', :q, '%')))
           OR (:field = 'matchType' AND LOWER(COALESCE(r.matchType, '')) LIKE LOWER(CONCAT('%', :q, '%')))
           OR (:field = 'cpu' AND LOWER(COALESCE(r.cpu, '')) LIKE LOWER(CONCAT('%', :q, '%')))
        """,
    )
    fun search(q: String, field: String, pageable: Pageable): Page<ReplayEntity>
}

@Repository
interface ReplayPlayerRepository : JpaRepository<ReplayPlayerEntity, String> {
    fun findByReplayIdIn(replayIds: Collection<String>): List<ReplayPlayerEntity>
}

@Repository
interface ReplayAssociatedFileRepository : JpaRepository<ReplayAssociatedFileEntity, String> {
    fun findByReplayIdIn(replayIds: Collection<String>): List<ReplayAssociatedFileEntity>
}

@Repository
interface PlayerHardwareRepository : JpaRepository<PlayerHardwareEntity, String> {
    @Query(
        """
                SELECT h FROM PlayerHardwareEntity h
                WHERE (:field = 'all' AND (
                             LOWER(h.mainName) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(h.playerId) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(h.aliasesJson) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(h.cpu, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(h.cpuBenchmarkName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(string(h.cpuMatchStatus)) LIKE LOWER(CONCAT('%', :q, '%'))
                     ))
                    OR (:field = 'player' AND (
                             LOWER(h.mainName) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(h.playerId) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(h.aliasesJson) LIKE LOWER(CONCAT('%', :q, '%'))
                     ))
                    OR (:field = 'cpu' AND LOWER(COALESCE(h.cpu, '')) LIKE LOWER(CONCAT('%', :q, '%')))
                    OR (:field = 'benchmark' AND LOWER(COALESCE(h.cpuBenchmarkName, '')) LIKE LOWER(CONCAT('%', :q, '%')))
                    OR (:field = 'status' AND LOWER(string(h.cpuMatchStatus)) LIKE LOWER(CONCAT('%', :q, '%')))
          """,
     )
     fun search(q: String, field: String, pageable: Pageable): Page<PlayerHardwareEntity>

     fun countByCpuScoreIsNotNull(): Long

     fun countByCpuMatchStatus(status: CpuMatchStatus): Long
}

@Repository
interface ReplayCollectionJobRepository : JpaRepository<ReplayCollectionJobEntity, String> {
    fun findTop20ByOrderByCreatedAtDesc(): List<ReplayCollectionJobEntity>
}