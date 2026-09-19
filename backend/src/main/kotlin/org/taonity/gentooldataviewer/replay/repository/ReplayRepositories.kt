package org.taonity.gentooldataviewer.replay.repository

import org.taonity.gentooldataviewer.replay.entity.PlayerHardwareEntity
import org.taonity.gentooldataviewer.replay.entity.GentoolUserLinkEntity
import org.taonity.gentooldataviewer.replay.entity.GentoolUserLinkId
import org.taonity.gentooldataviewer.replay.entity.GentoolLinkStatus
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

interface ReplayMatchDateBounds {
    val firstMatchAt: Instant?
    val lastMatchAt: Instant?
}

@Repository
interface ReplayRepository : JpaRepository<ReplayEntity, String> {
    fun existsBySourceUrl(sourceUrl: String): Boolean

    fun findByPlayerNames(playerNames: String): List<ReplayEntity>

    fun findByReporterId(reporterId: String): List<ReplayEntity>

    @Query(
        """
        SELECT r FROM ReplayEntity r
        WHERE r.reporterId IN :reporterIds
          AND r.matchAt = (
              SELECT MAX(latest.matchAt) FROM ReplayEntity latest
              WHERE latest.reporterId = r.reporterId
          )
        """,
    )
    fun findLatestByReporterIds(reporterIds: Collection<String>): List<ReplayEntity>

    @Query("SELECT MIN(r.collectedAt) FROM ReplayEntity r")
    fun findEarliestCollectedAt(): Instant?

    @Query("SELECT MIN(r.matchAt) AS firstMatchAt, MAX(r.matchAt) AS lastMatchAt FROM ReplayEntity r")
    fun findMatchDateBounds(): ReplayMatchDateBounds

    @Query(
        """
        SELECT DISTINCT r FROM ReplayEntity r
        LEFT JOIN ReplayPlayerEntity p ON p.replayId = r.id
                    WHERE (:replayId IS NULL OR r.id = :replayId)
                        AND (:filterReporterIds = false OR r.reporterId IN :reporterIds)
                        AND (cast(:fromDate as Instant) IS NULL OR r.matchAt >= :fromDate)
                        AND (cast(:untilDate as Instant) IS NULL OR r.matchAt < :untilDate)
                        AND ((:field = 'all' AND (
                        LOWER(cast(r.matchAt as String)) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                      OR LOWER(r.reporterName) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                      OR LOWER(r.reporterId) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                      OR LOWER(p.name) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                      OR LOWER(COALESCE(r.mapName, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                      OR LOWER(COALESCE(r.matchType, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                     OR LOWER(COALESCE(cast(r.matchLengthSeconds as String), '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                      OR LOWER(COALESCE(r.cpu, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                      OR LOWER(COALESCE(r.matchMode, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                     OR LOWER(COALESCE(cast(r.startCash as String), '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                      OR LOWER(COALESCE(r.gentoolVersion, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                      OR LOWER(COALESCE(r.gameVersion, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                      OR LOWER(COALESCE(r.windowsCompat, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                      OR LOWER(COALESCE(r.repInfoInUse, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                         OR LOWER(COALESCE(cast(r.replaySizeBytes as String), '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                         OR LOWER(cast(r.sourceDate as String)) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                         OR LOWER(cast(r.collectedAt as String)) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                 ))
                     OR (:field = 'matchAt' AND LOWER(cast(r.matchAt as String)) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
              OR (:field = 'reporter' AND (
                          LOWER(r.reporterName) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                      OR LOWER(r.reporterId) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                 ))
              OR (:field = 'players' AND LOWER(p.name) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
           OR (:field = 'mapName' AND LOWER(COALESCE(r.mapName, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
           OR (:field = 'matchType' AND LOWER(COALESCE(r.matchType, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
               OR (:field = 'duration' AND LOWER(COALESCE(cast(r.matchLengthSeconds as String), '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
           OR (:field = 'cpu' AND LOWER(COALESCE(r.cpu, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
              OR (:field = 'matchMode' AND LOWER(COALESCE(r.matchMode, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
               OR (:field = 'startCash' AND LOWER(COALESCE(cast(r.startCash as String), '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
              OR (:field = 'gentoolVersion' AND LOWER(COALESCE(r.gentoolVersion, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
              OR (:field = 'gameVersion' AND LOWER(COALESCE(r.gameVersion, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
              OR (:field = 'windowsCompat' AND LOWER(COALESCE(r.windowsCompat, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
              OR (:field = 'repInfoInUse' AND LOWER(COALESCE(r.repInfoInUse, '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
               OR (:field = 'replaySize' AND LOWER(COALESCE(cast(r.replaySizeBytes as String), '')) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
               OR (:field = 'sourceDate' AND LOWER(cast(r.sourceDate as String)) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END))
               OR (:field = 'collectedAt' AND LOWER(cast(r.collectedAt as String)) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)))
        """,
    )
    fun search(
        q: String,
        field: String,
        pageable: Pageable,
        reporterIds: Collection<String> = listOf(""),
        filterReporterIds: Boolean = false,
        replayId: String? = null,
        exact: Boolean = false,
        fromDate: Instant? = null,
        untilDate: Instant? = null,
    ): Page<ReplayEntity>
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
                WHERE (:playerId IS NULL OR h.playerId = :playerId)
                AND (:linkedOnly = false OR EXISTS (
                    SELECT l.userId FROM GentoolUserLinkEntity l
                    WHERE l.playerId = h.playerId AND l.status = :linkStatus
                ))
                AND ((:exact = false AND ((:field = 'all' AND (
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
                    OR (:field = 'scoreUpdatedAt' AND LOWER(COALESCE(cast(h.cpuScoreUpdatedAt as String), '')) LIKE LOWER(CONCAT('%', :q, '%')))))
                    OR (:exact = true AND ((:field = 'all' AND (
                           LOWER(h.mainName) = LOWER(:q)
                        OR LOWER(h.playerId) = LOWER(:q)
                        OR LOWER(h.aliasesJson) LIKE LOWER(CONCAT('%"', :q, '"%'))
                        OR LOWER(cast(h.replayCount as String)) = LOWER(:q)
                        OR LOWER(COALESCE(h.cpu, '')) = LOWER(:q)
                        OR LOWER(COALESCE(cast(h.cpuScore as String), '')) = LOWER(:q)
                        OR LOWER(h.latestName) = LOWER(:q)
                        OR LOWER(COALESCE(h.cpuBenchmarkName, '')) = LOWER(:q)
                        OR LOWER(string(h.cpuMatchStatus)) = LOWER(:q)
                        OR LOWER(COALESCE(cast(h.gentoolUpdatedAt as String), '')) = LOWER(:q)
                        OR LOWER(COALESCE(cast(h.cpuScoreUpdatedAt as String), '')) = LOWER(:q)
                    ))
                    OR (:field = 'mainName' AND LOWER(h.mainName) = LOWER(:q))
                    OR (:field = 'playerId' AND LOWER(h.playerId) = LOWER(:q))
                    OR (:field = 'aliases' AND LOWER(h.aliasesJson) LIKE LOWER(CONCAT('%"', :q, '"%')))
                    OR (:field = 'replayCount' AND LOWER(cast(h.replayCount as String)) = LOWER(:q))
                    OR (:field = 'reportedCpu' AND LOWER(COALESCE(h.cpu, '')) = LOWER(:q))
                    OR (:field = 'score' AND LOWER(COALESCE(cast(h.cpuScore as String), '')) = LOWER(:q))
                    OR (:field = 'latestName' AND LOWER(h.latestName) = LOWER(:q))
                    OR (:field = 'benchmark' AND LOWER(COALESCE(h.cpuBenchmarkName, '')) = LOWER(:q))
                    OR (:field = 'status' AND LOWER(string(h.cpuMatchStatus)) = LOWER(:q))
                    OR (:field = 'gentoolUpdatedAt' AND LOWER(COALESCE(cast(h.gentoolUpdatedAt as String), '')) = LOWER(:q))
                    OR (:field = 'scoreUpdatedAt' AND LOWER(COALESCE(cast(h.cpuScoreUpdatedAt as String), '')) = LOWER(:q)))))
          """,
     )
     fun search(
         q: String,
         field: String,
         pageable: Pageable,
         linkedOnly: Boolean = false,
         linkStatus: GentoolLinkStatus = GentoolLinkStatus.APPROVED,
         playerId: String? = null,
         exact: Boolean = false,
     ): Page<PlayerHardwareEntity>

     @Query(
         """
                SELECT h FROM PlayerHardwareEntity h
                LEFT JOIN GentoolUserLinkEntity l ON l.playerId = h.playerId AND l.status = :linkStatus
                LEFT JOIN UserEntity u ON u.googleId = l.userId
                WHERE (:playerId IS NULL OR h.playerId = :playerId)
                AND (:linkedOnly = false OR l.userId IS NOT NULL)
                AND ((:exact = false AND ((:field = 'all' AND (
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
                    OR (:field = 'scoreUpdatedAt' AND LOWER(COALESCE(cast(h.cpuScoreUpdatedAt as String), '')) LIKE LOWER(CONCAT('%', :q, '%')))))
                    OR (:exact = true AND ((:field = 'all' AND (
                           LOWER(h.mainName) = LOWER(:q)
                        OR LOWER(h.playerId) = LOWER(:q)
                        OR LOWER(h.aliasesJson) LIKE LOWER(CONCAT('%"', :q, '"%'))
                        OR LOWER(cast(h.replayCount as String)) = LOWER(:q)
                        OR LOWER(COALESCE(h.cpu, '')) = LOWER(:q)
                        OR LOWER(COALESCE(cast(h.cpuScore as String), '')) = LOWER(:q)
                        OR LOWER(h.latestName) = LOWER(:q)
                        OR LOWER(COALESCE(h.cpuBenchmarkName, '')) = LOWER(:q)
                        OR LOWER(string(h.cpuMatchStatus)) = LOWER(:q)
                        OR LOWER(COALESCE(cast(h.gentoolUpdatedAt as String), '')) = LOWER(:q)
                        OR LOWER(COALESCE(cast(h.cpuScoreUpdatedAt as String), '')) = LOWER(:q)
                    ))
                    OR (:field = 'mainName' AND LOWER(h.mainName) = LOWER(:q))
                    OR (:field = 'playerId' AND LOWER(h.playerId) = LOWER(:q))
                    OR (:field = 'aliases' AND LOWER(h.aliasesJson) LIKE LOWER(CONCAT('%"', :q, '"%')))
                    OR (:field = 'replayCount' AND LOWER(cast(h.replayCount as String)) = LOWER(:q))
                    OR (:field = 'reportedCpu' AND LOWER(COALESCE(h.cpu, '')) = LOWER(:q))
                    OR (:field = 'score' AND LOWER(COALESCE(cast(h.cpuScore as String), '')) = LOWER(:q))
                    OR (:field = 'latestName' AND LOWER(h.latestName) = LOWER(:q))
                    OR (:field = 'benchmark' AND LOWER(COALESCE(h.cpuBenchmarkName, '')) = LOWER(:q))
                    OR (:field = 'status' AND LOWER(string(h.cpuMatchStatus)) = LOWER(:q))
                    OR (:field = 'gentoolUpdatedAt' AND LOWER(COALESCE(cast(h.gentoolUpdatedAt as String), '')) = LOWER(:q))
                    OR (:field = 'scoreUpdatedAt' AND LOWER(COALESCE(cast(h.cpuScoreUpdatedAt as String), '')) = LOWER(:q)))))
                ORDER BY
                    CASE WHEN u.displayName IS NULL THEN 1 ELSE 0 END,
                    CASE WHEN :ascending = true THEN LOWER(u.displayName) END ASC,
                    CASE WHEN :ascending = false THEN LOWER(u.displayName) END DESC,
                    LOWER(h.mainName) ASC,
                    h.playerId ASC
          """,
     )
     fun searchSortedByDiscord(
         q: String,
         field: String,
         linkStatus: GentoolLinkStatus,
         ascending: Boolean,
         pageable: Pageable,
         linkedOnly: Boolean = false,
         playerId: String? = null,
         exact: Boolean = false,
     ): Page<PlayerHardwareEntity>

     fun countByCpuScoreIsNotNull(): Long

     fun countByCpuMatchStatus(status: CpuMatchStatus): Long
}

@Repository
interface ReplayCollectionJobRepository : JpaRepository<ReplayCollectionJobEntity, String> {
    fun findTop20ByOrderByCreatedAtDesc(): List<ReplayCollectionJobEntity>

    fun findAllByStatusIn(statuses: Collection<CollectionStatus>): List<ReplayCollectionJobEntity>
}

@Repository
interface GentoolUserLinkRepository : JpaRepository<GentoolUserLinkEntity, GentoolUserLinkId> {
    fun findByPlayerId(playerId: String): GentoolUserLinkEntity?

    fun findByUserId(userId: String): List<GentoolUserLinkEntity>

    fun findByUserIdIn(userIds: Collection<String>): List<GentoolUserLinkEntity>

    fun findByUserIdAndPlayerId(userId: String, playerId: String): GentoolUserLinkEntity?

    fun countByUserId(userId: String): Long

    fun findByPlayerIdIn(playerIds: Collection<String>): List<GentoolUserLinkEntity>

    fun findByPlayerIdInAndStatus(
        playerIds: Collection<String>,
        status: GentoolLinkStatus,
    ): List<GentoolUserLinkEntity>
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