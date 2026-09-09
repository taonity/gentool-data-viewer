package org.taonity.gentooldataviewer.cpu.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.console.config.ConsolePagingProperties
import org.taonity.gentooldataviewer.cpu.repository.CpuBenchmarkRepository
import org.taonity.gentooldataviewer.replay.entity.GentoolLinkStatus
import org.taonity.gentooldataviewer.replay.entity.GentoolUserLinkEntity
import org.taonity.gentooldataviewer.replay.entity.PlayerHardwareEntity
import org.taonity.gentooldataviewer.replay.repository.GentoolUserLinkRepository
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRepository
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.repository.UserRepository
import tools.jackson.databind.ObjectMapper
import java.time.Instant

class CpuPlayerQueryServiceTest {
    private val hardwareRepository = mock(PlayerHardwareRepository::class.java)
    private val replayRepository = mock(ReplayRepository::class.java)
    private val linkRepository = mock(GentoolUserLinkRepository::class.java)
    private val userRepository = mock(UserRepository::class.java)
    private val benchmarkRepository = mock(CpuBenchmarkRepository::class.java)
    private val benchmarkSyncService = mock(CpuBenchmarkSyncService::class.java)
    private val settings = mock(AppSettings::class.java)
    private val service = CpuPlayerQueryService(
        hardwareRepository,
        replayRepository,
        linkRepository,
        userRepository,
        benchmarkRepository,
        benchmarkSyncService,
        settings,
        ObjectMapper(),
    )

    @Test
    fun `player list exposes Discord identity only for approved links`() {
        val approvedPlayer = player("ABCDEF123456", "Approved player")
        val unlinkedPlayer = player("123456ABCDEF", "Unlinked player")
        val pageRequest = PageRequest.of(0, 50)
        val expectedPageable = PageRequest.of(
            0,
            50,
            Sort.by(
                Sort.Order.desc("cpuScore").nullsLast(),
                Sort.Order.asc("mainName"),
                Sort.Order.asc("playerId"),
            ),
        )
        val approvedLink = GentoolUserLinkEntity(
            userId = "discord:100",
            playerId = approvedPlayer.playerId,
            status = GentoolLinkStatus.APPROVED,
        )
        val discordUser = UserEntity(
            googleId = approvedLink.userId,
            authProvider = "discord",
            displayName = "Discord Player",
            pictureUrl = "https://cdn.discordapp.com/avatars/100/avatar.png",
        )
        `when`(settings.console()).thenReturn(
            ConsolePagingProperties(maxPageSize = 100, accessRequestsEnabled = false),
        )
        `when`(hardwareRepository.search("", "all", expectedPageable))
            .thenReturn(PageImpl(listOf(approvedPlayer, unlinkedPlayer), pageRequest, 2))
        `when`(
            linkRepository.findByPlayerIdInAndStatus(
                listOf(approvedPlayer.playerId, unlinkedPlayer.playerId),
                GentoolLinkStatus.APPROVED,
            ),
        ).thenReturn(listOf(approvedLink))
        `when`(userRepository.findAllById(listOf(approvedLink.userId))).thenReturn(listOf(discordUser))

        val result = service.list(null, null, 0, 50, null, null)

        assertThat(result.content[0].discordUser).isNotNull
        assertThat(result.content[0].discordUser?.displayName).isEqualTo("Discord Player")
        assertThat(result.content[0].discordUser?.pictureUrl).isEqualTo(discordUser.pictureUrl)
        assertThat(result.content[1].discordUser).isNull()
        verify(linkRepository).findByPlayerIdInAndStatus(
            listOf(approvedPlayer.playerId, unlinkedPlayer.playerId),
            GentoolLinkStatus.APPROVED,
        )
    }

    private fun player(playerId: String, name: String) = PlayerHardwareEntity(
        playerId = playerId,
        latestName = name,
        observedAt = Instant.parse("2026-09-01T12:00:00Z"),
        sourceReplayId = "replay-$playerId",
    )
}