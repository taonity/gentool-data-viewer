package org.taonity.gentooldataviewer.replay.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import org.taonity.gentooldataviewer.replay.repository.ReplayRetentionRepository
import org.taonity.gentooldataviewer.replay.repository.RetainedPlayerName
import org.taonity.gentooldataviewer.replay.repository.RetainedPlayerSummary
import tools.jackson.databind.ObjectMapper

data class ReplayRetentionResult(val replaysDeleted: Long, val hardwareDeleted: Long)

@Service
class ReplayRetentionService(
    private val repository: ReplayRetentionRepository,
    private val policy: ReplayRetentionPolicy,
    private val objectMapper: ObjectMapper,
    transactionManager: PlatformTransactionManager,
) {
    private val transaction = TransactionTemplate(transactionManager).apply {
        propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW
    }

    fun cleanup(): ReplayRetentionResult {
        val cutoff = policy.cutoff()
        var hardwareDeleted = 0L
        var replaysDeleted = 0L
        while (true) {
            val removed = transaction.execute {
                repository.lock()
                val ids = repository.expiredHardware(cutoff, BATCH_SIZE)
                if (ids.isEmpty()) 0 else repository.deleteHardware(ids)
            } ?: 0
            hardwareDeleted += removed
            if (removed == 0) break
        }
        while (true) {
            val removed = transaction.execute {
                repository.lock()
                val expired = repository.expiredReplays(cutoff, BATCH_SIZE)
                if (expired.isEmpty()) return@execute ReplayRetentionResult(0, 0)
                val ids = expired.map { it.id }
                // Normally removed above; also covers legacy rows whose observation/source dates disagree.
                val hardware = repository.deleteSourceHardware(ids)
                val replays = repository.deleteReplays(ids)
                val summaries = repository.retainedNames(expired.map { it.reporterId }.toSet(), cutoff)
                    .groupBy { it.playerId }.map { (playerId, names) ->
                        val ordered = names.sortedWith(compareByDescending<RetainedPlayerName> { it.count }
                            .thenByDescending { it.latestAt }.thenBy { it.name })
                        RetainedPlayerSummary(playerId, ordered.first().name,
                            objectMapper.writeValueAsString(ordered.drop(1).map { it.name }), ordered.sumOf { it.count })
                    }
                repository.updateSummaries(summaries)
                ReplayRetentionResult(replays.toLong(), hardware.toLong())
            } ?: break
            replaysDeleted += removed.replaysDeleted
            hardwareDeleted += removed.hardwareDeleted
            if (removed.replaysDeleted == 0L) break
        }
        LOGGER.info { "Replay retention cleanup: removed $replaysDeleted replays and $hardwareDeleted hardware rows before $cutoff" }
        return ReplayRetentionResult(replaysDeleted, hardwareDeleted)
    }

    private companion object {
        const val BATCH_SIZE = 500
        val LOGGER = KotlinLogging.logger {}
    }
}
