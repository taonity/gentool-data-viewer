package org.taonity.gentooldataviewer.replay.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.taonity.gentooldataviewer.console.entity.AuditAction
import org.taonity.gentooldataviewer.console.repository.AuditLogRepository
import org.taonity.gentooldataviewer.replay.entity.GentoolLinkStatus
import org.taonity.gentooldataviewer.replay.entity.PlayerHardwareEntity
import org.taonity.gentooldataviewer.replay.entity.ReplayCollectionJobEntity
import org.taonity.gentooldataviewer.replay.entity.ReplayEntity
import org.taonity.gentooldataviewer.replay.entity.CollectionTrigger
import org.taonity.gentooldataviewer.replay.repository.GentoolUserLinkRepository
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayCollectionJobRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRescanRequestRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRepository
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserInfo
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserPrincipal
import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@SpringBootTest
@ActiveProfiles("h2")
class ReplayRescanServiceTest {
    @Autowired lateinit var service: ReplayRescanService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var hardwareRepository: PlayerHardwareRepository
    @Autowired lateinit var linkRepository: GentoolUserLinkRepository
    @Autowired lateinit var requestRepository: ReplayRescanRequestRepository
    @Autowired lateinit var jobRepository: ReplayCollectionJobRepository
    @Autowired lateinit var replayRepository: ReplayRepository
    @Autowired lateinit var auditRepository: AuditLogRepository
    @MockitoBean lateinit var coordinator: ReplayCollectionCoordinator

    private val userId = "discord:900000000000000001"
    private val playerId = "ABCDEF123456"
    private val otherPlayerId = "123456ABCDEF"

    @BeforeEach
    fun prepare() {
        requestRepository.deleteAll()
        jobRepository.deleteAll()
        linkRepository.deleteAll()
        auditRepository.deleteAll()
        hardwareRepository.deleteAll()
        replayRepository.deleteAll()
        userRepository.deleteAll()
        userRepository.save(
            UserEntity(
                googleId = userId,
                authProvider = "discord",
                displayName = "Viewer",
                role = ConsoleRole.VIEWER,
                accessStatus = AccessRequestStatus.APPROVED,
            )
        )
        listOf(playerId, otherPlayerId).forEachIndexed { index, id ->
            val replay = replayRepository.save(
                ReplayEntity(
                    sourceUrl = "https://example.invalid/replay-$index.txt",
                    sourceDate = LocalDate.now(),
                    reporterId = id,
                    reporterName = "Player $index",
                    playerNames = "Player $index",
                    matchAt = Instant.now(),
                    fieldsJson = "{}",
                    rawText = "fixture",
                )
            )
            hardwareRepository.save(
                PlayerHardwareEntity(
                    playerId = id,
                    latestName = "Player $index",
                    mainName = "Player $index",
                    observedAt = Instant.now(),
                    sourceReplayId = requireNotNull(replay.id),
                )
            )
        }
        Mockito.reset(coordinator)
    }

    @Test
    fun `claim is immediately approved and matching rescan is own`() {
        val principal = principal()
        val requested = service.requestLink(principal, playerId.lowercase())
        assertThat(requested.status).isEqualTo(GentoolLinkStatus.APPROVED)

        val jobId = queuedJob(playerId)
        stubCoordinator(playerId, jobId)
        val accepted = service.requestRescan(principal, playerId)

        val dashboard = service.dashboard(principal)
        assertThat(accepted.ownTarget).isTrue()
        assertThat(dashboard.link?.status).isEqualTo(GentoolLinkStatus.APPROVED)
        assertThat(dashboard.history.single().ownTarget).isTrue()
        assertThat(dashboard.otherUsedToday).isZero()
        assertThat(auditRepository.findAll().map { it.action })
            .contains(AuditAction.CLAIM_GENTOOL_LINK, AuditAction.REQUEST_RESCAN)
    }

    @Test
    fun `claiming another player immediately replaces own link`() {
        val principal = principal()
        service.requestLink(principal, playerId)

        val reclaimed = service.requestLink(principal, otherPlayerId)

        assertThat(reclaimed.status).isEqualTo(GentoolLinkStatus.APPROVED)
        assertThat(reclaimed.playerId).isEqualTo(otherPlayerId)
        assertThat(linkRepository.findById(userId).orElseThrow().playerId).isEqualTo(otherPlayerId)
        assertThat(auditRepository.findAll().map { it.action })
            .contains(AuditAction.CLAIM_GENTOOL_LINK, AuditAction.RECLAIM_GENTOOL_LINK)
    }

    @Test
    fun `user without console role can manage own link`() {
        val user = userRepository.findById(userId).orElseThrow()
        user.role = ConsoleRole.NONE
        user.accessStatus = AccessRequestStatus.NONE
        userRepository.save(user)

        val claimed = service.requestLink(principal(), playerId)
        assertThat(claimed.playerId).isEqualTo(playerId)
        assertThat(service.dashboard(principal()).link?.playerId).isEqualTo(playerId)

        service.unlink(principal())

        assertThat(service.dashboard(principal()).link).isNull()
        assertThat(auditRepository.findAll().map { it.action })
            .contains(AuditAction.CLAIM_GENTOOL_LINK, AuditAction.UNLINK_GENTOOL_LINK)
    }

    @Test
    fun `recent other target consumes quota and is blocked by cooldown`() {
        val principal = principal()
        val jobId = queuedJob(otherPlayerId)
        stubCoordinator(otherPlayerId, jobId)
        val accepted = service.requestRescan(principal, otherPlayerId)

        assertThat(accepted.ownTarget).isFalse()
        assertThat(service.dashboard(principal).otherUsedToday).isEqualTo(1)
        assertThatThrownBy { service.requestRescan(principal, otherPlayerId) }
            .isInstanceOf(ResponseStatusException::class.java)
            .hasMessageContaining("recently")
    }

    @Test
    fun `admins and owners receive privileged rescan limits`() {
        for (role in listOf(ConsoleRole.ADMIN, ConsoleRole.OWNER)) {
            val user = userRepository.findById(userId).orElseThrow()
            user.role = role
            userRepository.save(user)

            val dashboard = service.dashboard(principal())

            assertThat(dashboard.otherDailyLimit).isEqualTo(100)
            assertThat(dashboard.targetCooldownSeconds).isEqualTo(60)
        }
    }

    private fun queuedJob(targetPlayerId: String): String = requireNotNull(
        jobRepository.save(
            ReplayCollectionJobEntity(
                triggerType = CollectionTrigger.USER_RESCAN,
                startDate = LocalDate.now(),
                endDate = LocalDate.now(),
                requestedBy = userId,
                targetPlayerId = targetPlayerId,
            )
        ).id
    )

    private fun stubCoordinator(targetPlayerId: String, jobId: String) {
        val endDate = LocalDate.now(ZoneOffset.UTC)
        Mockito.doReturn(jobId).`when`(coordinator).startUserRescan(
            endDate.minusDays(6),
            endDate,
            userId,
            targetPlayerId,
        )
    }

    private fun principal(): AuthenticatedUserPrincipal {
        val attributes = mapOf<String, Any>("id" to "900000000000000001")
        val oauthUser = DefaultOAuth2User(
            listOf(SimpleGrantedAuthority("ROLE_USER")),
            attributes,
            "id",
        )
        return AuthenticatedUserPrincipal.of(
            AuthenticatedUserInfo("discord", "900000000000000001", "Viewer", null),
            oauthUser,
        )
    }
}