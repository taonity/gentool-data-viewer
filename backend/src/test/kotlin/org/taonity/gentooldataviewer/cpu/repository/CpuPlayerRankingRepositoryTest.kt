package org.taonity.gentooldataviewer.cpu.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.taonity.gentooldataviewer.replay.entity.GentoolLinkStatus
import org.taonity.gentooldataviewer.replay.entity.GentoolUserLinkEntity
import org.taonity.gentooldataviewer.replay.entity.PlayerHardwareEntity
import org.taonity.gentooldataviewer.replay.entity.ReplayEntity
import org.taonity.gentooldataviewer.replay.repository.GentoolUserLinkRepository
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRepository
import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.repository.UserRepository
import java.time.Instant
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("h2")
class CpuPlayerRankingRepositoryTest {
    @Autowired lateinit var rankingRepository: CpuPlayerRankingRepository
    @Autowired lateinit var playerRepository: PlayerHardwareRepository
    @Autowired lateinit var replayRepository: ReplayRepository
    @Autowired lateinit var linkRepository: GentoolUserLinkRepository
    @Autowired lateinit var userRepository: UserRepository

    @BeforeEach
    fun prepare() {
        linkRepository.deleteAll()
        playerRepository.deleteAll()
        replayRepository.deleteAll()
        userRepository.deleteAll()
        userRepository.save(
            UserEntity(
                googleId = USER_ID,
                authProvider = "discord",
                displayName = "Ranked user",
                role = ConsoleRole.VIEWER,
                accessStatus = AccessRequestStatus.APPROVED,
            ),
        )
        listOf(
            Triple("AAA000000001", "Alpha", 10L),
            Triple("BBB000000002", "Bravo", 30L),
            Triple("CCC000000003", "Charlie", 20L),
        ).forEachIndexed { index, (playerId, name, games) ->
            val replay = replayRepository.save(
                ReplayEntity(
                    sourceUrl = "https://example.invalid/rank-$index.txt",
                    sourceDate = LocalDate.parse("2026-09-01"),
                    reporterId = playerId,
                    reporterName = name,
                    playerNames = name,
                    matchAt = Instant.parse("2026-09-01T12:00:00Z"),
                    fieldsJson = "{}",
                    rawText = "fixture",
                ),
            )
            playerRepository.save(
                PlayerHardwareEntity(
                    playerId = playerId,
                    latestName = name,
                    replayCount = games,
                    observedAt = Instant.parse("2026-09-01T12:00:00Z"),
                    sourceReplayId = requireNotNull(replay.id),
                ),
            )
        }
        linkRepository.saveAll(
            listOf(
                GentoolUserLinkEntity(USER_ID, "AAA000000001", GentoolLinkStatus.APPROVED),
                GentoolUserLinkEntity(USER_ID, "CCC000000003", GentoolLinkStatus.APPROVED),
            ),
        )
    }

    @AfterEach
    fun cleanUp() {
        linkRepository.deleteAll()
        playerRepository.deleteAll()
        replayRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `ranks players by games and respects linked-only filter`() {
        assertThat(playerRepository.search("30", "replayCount", PageRequest.of(0, 10), exact = true).content)
            .extracting<String> { it.playerId }
            .containsExactly("BBB000000002")
        assertThat(playerRepository.search("3", "replayCount", PageRequest.of(0, 10), exact = true).content)
            .isEmpty()

        assertThat(
            rankingRepository.findRanks(
                listOf("AAA000000001", "CCC000000003"),
                "replayCount",
                "desc",
                linkedOnly = false,
            ),
        ).containsExactlyInAnyOrderEntriesOf(
            mapOf("AAA000000001" to 3L, "CCC000000003" to 2L),
        )

        assertThat(
            rankingRepository.findRanks(
                listOf("AAA000000001", "CCC000000003"),
                "replayCount",
                "desc",
                linkedOnly = true,
            ),
        ).containsExactlyInAnyOrderEntriesOf(
            mapOf("AAA000000001" to 2L, "CCC000000003" to 1L),
        )
    }

    private companion object {
        const val USER_ID = "discord:800000000000000001"
    }
}