package org.taonity.gentooldataviewer.replay.repository

import org.taonity.gentooldataviewer.replay.entity.PlayerHardwareEntity
import org.taonity.gentooldataviewer.replay.entity.GentoolUserLinkEntity
import org.taonity.gentooldataviewer.replay.entity.ReplayRescanRequestEntity
import org.taonity.gentooldataviewer.replay.entity.ReplayCollectionJobEntity
import org.taonity.gentooldataviewer.replay.entity.CollectionStatus
import org.taonity.gentooldataviewer.replay.entity.ReplayAssociatedFileEntity
import org.taonity.gentooldataviewer.replay.entity.ReplayEntity
import org.taonity.gentooldataviewer.replay.entity.ReplayPlayerEntity
import org.taonity.gentooldataviewer.cpu.service.CpuMatchStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.time.Instant

@Repository
interface ReplayRepository : JpaRepository<ReplayEntity, String> {
    fun existsBySourceUrl(sourceUrl: String): Boolean

    fun findByReporterId(reporterId: String): List<ReplayEntity>

    @Query("SELECT MIN(r.collectedAt) FROM ReplayEntity r")
    fun findEarliestCollectedAt(): Instant?

    @Query(
        """
        SELECT DISTINCT r FROM ReplayEntity r
        LEFT JOIN ReplayPlayerEntity p ON p.replayId = r.id
          WHERE (:field = 'all' AND (
                        LOWER(cast(r.matchAt as String)) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(r.reporterName) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(r.reporterId) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(COALESCE(r.mapName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(COALESCE(r.matchType, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                     OR LOWER(COALESCE(cast(r.matchLengthSeconds as String), '')) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(COALESCE(r.cpu, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(COALESCE(r.matchMode, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                     OR LOWER(COALESCE(cast(r.startCash as String), '')) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(COALESCE(r.gentoolVersion, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(COALESCE(r.gameVersion, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(COALESCE(r.windowsCompat, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(COALESCE(r.repInfoInUse, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(cast(r.replaySizeBytes as String), '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(cast(r.sourceDate as String)) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(cast(r.collectedAt as String)) LIKE LOWER(CONCAT('%', :q, '%'))
                 ))
                     OR (:field = 'matchAt' AND LOWER(cast(r.matchAt as String)) LIKE LOWER(CONCAT('%', :q, '%')))
              OR (:field = 'reporter' AND (
                          LOWER(r.reporterName) LIKE LOWER(CONCAT('%', :q, '%'))
                      OR LOWER(r.reporterId) LIKE LOWER(CONCAT('%', :q, '%'))
                 ))
              OR (:field = 'players' AND LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')))
           OR (:field = 'mapName' AND LOWER(COALESCE(r.mapName, '')) LIKE LOWER(CONCAT('%', :q, '%')))
           OR (:field = 'matchType' AND LOWER(COALESCE(r.matchType, '')) LIKE LOWER(CONCAT('%', :q, '%')))
               OR (:field = 'duration' AND LOWER(COALESCE(cast(r.matchLengthSeconds as String), '')) LIKE LOWER(CONCAT('%', :q, '%')))
           OR (:field = 'cpu' AND LOWER(COALESCE(r.cpu, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              OR (:field = 'matchMode' AND LOWER(COALESCE(r.matchMode, '')) LIKE LOWER(CONCAT('%', :q, '%')))
               OR (:field = 'startCash' AND LOWER(COALESCE(cast(r.startCash as String), '')) LIKE LOWER(CONCAT('%', :q, '%')))
              OR (:field = 'gentoolVersion' AND LOWER(COALESCE(r.gentoolVersion, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              OR (:field = 'gameVersion' AND LOWER(COALESCE(r.gameVersion, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              OR (:field = 'windowsCompat' AND LOWER(COALESCE(r.windowsCompat, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              OR (:field = 'repInfoInUse' AND LOWER(COALESCE(r.repInfoInUse, '')) LIKE LOWER(CONCAT('%', :q, '%')))
               OR (:field = 'replaySize' AND LOWER(COALESCE(cast(r.replaySizeBytes as String), '')) LIKE LOWER(CONCAT('%', :q, '%')))
               OR (:field = 'sourceDate' AND LOWER(cast(r.sourceDate as String)) LIKE LOWER(CONCAT('%', :q, '%')))
               OR (:field = 'collectedAt' AND LOWER(cast(r.collectedAt as String)) LIKE LOWER(CONCAT('%', :q, '%')))
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
                         OR LOWER(cast(h.replayCount as String)) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(h.cpu, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(cast(h.cpuScore as String), '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(h.latestName) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(h.cpuBenchmarkName, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(string(h.cpuMatchStatus)) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(cast(h.gentoolUpdatedAt as String), '')) LIKE LOWER(CONCAT('%', :q, '%'))
                         OR LOWER(COALESCE(cast(h.cpuScoreUpdatedAt as String), '')) LIKE LOWER(CONCAT('%', :q, '%'))
                     ))
                    OR (:field = 'mainName' AND LOWER(h.mainName) LIKE LOWER(CONCAT('%', :q, '%')))
                    OR (:field = 'playerId' AND LOWER(h.playerId) LIKE LOWER(CONCAT('%', :q, '%')))
                    OR (:field = 'aliases' AND LOWER(h.aliasesJson) LIKE LOWER(CONCAT('%', :q, '%')))
                    OR (:field = 'replayCount' AND LOWER(cast(h.replayCount as String)) LIKE LOWER(CONCAT('%', :q, '%')))
                    OR (:field = 'reportedCpu' AND LOWER(COALESCE(h.cpu, '')) LIKE LOWER(CONCAT('%', :q, '%')))
                    OR (:field = 'score' AND LOWER(COALESCE(cast(h.cpuScore as String), '')) LIKE LOWER(CONCAT('%', :q, '%')))
                    OR (:field = 'latestName' AND LOWER(h.latestName) LIKE LOWER(CONCAT('%', :q, '%')))
                    OR (:field = 'benchmark' AND LOWER(COALESCE(h.cpuBenchmarkName, '')) LIKE LOWER(CONCAT('%', :q, '%')))
                    OR (:field = 'status' AND LOWER(string(h.cpuMatchStatus)) LIKE LOWER(CONCAT('%', :q, '%')))
                    OR (:field = 'gentoolUpdatedAt' AND LOWER(COALESCE(cast(h.gentoolUpdatedAt as String), '')) LIKE LOWER(CONCAT('%', :q, '%')))
                    OR (:field = 'scoreUpdatedAt' AND LOWER(COALESCE(cast(h.cpuScoreUpdatedAt as String), '')) LIKE LOWER(CONCAT('%', :q, '%')))
          """,
     )
     fun search(q: String, field: String, pageable: Pageable): Page<PlayerHardwareEntity>

     fun countByCpuScoreIsNotNull(): Long

     fun countByCpuMatchStatus(status: CpuMatchStatus): Long
}

@Repository
interface ReplayCollectionJobRepository : JpaRepository<ReplayCollectionJobEntity, String> {
    fun findTop20ByOrderByCreatedAtDesc(): List<ReplayCollectionJobEntity>

    fun findAllByStatusIn(statuses: Collection<CollectionStatus>): List<ReplayCollectionJobEntity>
}

@Repository
interface GentoolUserLinkRepository : JpaRepository<GentoolUserLinkEntity, String> {
    fun findByPlayerId(playerId: String): GentoolUserLinkEntity?
}

@Repository
interface ReplayRescanRequestRepository : JpaRepository<ReplayRescanRequestEntity, String> {
    fun countByRequestedByUserIdAndOwnTargetFalseAndRequestedAtGreaterThanEqual(
        requestedByUserId: String,
        requestedAt: Instant,
    ): Long

    fun findTopByRequestedByUserIdAndTargetPlayerIdOrderByRequestedAtDesc(
        requestedByUserId: String,
        targetPlayerId: String,
    ): ReplayRescanRequestEntity?

    fun findTop10ByRequestedByUserIdOrderByRequestedAtDesc(requestedByUserId: String): List<ReplayRescanRequestEntity>
}