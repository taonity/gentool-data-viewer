package org.taonity.gentooldataviewer.replay.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.core.user.DefaultOAuth2User
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.taonity.gentooldataviewer.console.entity.AuditAction
import org.taonity.gentooldataviewer.console.repository.AuditLogRepository
import org.taonity.gentooldataviewer.replay.entity.GentoolLinkStatus
import org.taonity.gentooldataviewer.replay.entity.GentoolUserLinkEntity
import org.taonity.gentooldataviewer.replay.entity.PlayerHardwareEntity
import org.taonity.gentooldataviewer.replay.entity.ReplayEntity
import org.taonity.gentooldataviewer.replay.repository.GentoolUserLinkRepository
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRepository
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserInfo
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserPrincipal
import org.taonity.gentooldataviewer.security.client.DiscordBotClient
import org.taonity.gentooldataviewer.security.client.DiscordBotUser
import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.repository.UserRepository
import java.time.Instant
import java.time.LocalDate
import org.mockito.Mockito

@SpringBootTest
@ActiveProfiles("h2")
class GentoolLinkAdminServiceTest {
    @Autowired lateinit var service: GentoolLinkAdminService
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var playerRepository: PlayerHardwareRepository
    @Autowired lateinit var replayRepository: ReplayRepository
    @Autowired lateinit var linkRepository: GentoolUserLinkRepository
    @Autowired lateinit var auditRepository: AuditLogRepository
    @MockitoBean lateinit var discordBotClient: DiscordBotClient

    @BeforeEach
    fun prepare() {
        Mockito.reset(discordBotClient)
        linkRepository.deleteAll()
        auditRepository.deleteAll()
        playerRepository.deleteAll()
        replayRepository.deleteAll()
        userRepository.deleteAll()
        userRepository.saveAll(
            listOf(
                user(ADMIN_ID, "Admin", ConsoleRole.ADMIN),
                user(USER_A_ID, "Alpha Discord", ConsoleRole.VIEWER),
                user(USER_B_ID, "Bravo Discord", ConsoleRole.VIEWER),
            ),
        )
        listOf(PLAYER_A_ID, PLAYER_B_ID).forEachIndexed { index, playerId ->
            val replay = replayRepository.save(
                ReplayEntity(
                    sourceUrl = "https://example.invalid/admin-link-$index.txt",
                    sourceDate = LocalDate.now(),
                    reporterId = playerId,
                    reporterName = "Player $index",
                    playerNames = "Player $index",
                    matchAt = Instant.now(),
                    fieldsJson = "{}",
                    rawText = "fixture",
                ),
            )
            playerRepository.save(
                PlayerHardwareEntity(
                    playerId = playerId,
                    latestName = "Player $index",
                    mainName = "Player $index",
                    observedAt = Instant.now(),
                    sourceReplayId = requireNotNull(replay.id),
                ),
            )
        }
        linkRepository.saveAll(
            listOf(
                GentoolUserLinkEntity(USER_A_ID, PLAYER_A_ID, GentoolLinkStatus.APPROVED),
                GentoolUserLinkEntity(USER_B_ID, PLAYER_B_ID, GentoolLinkStatus.APPROVED),
            ),
        )
    }

    @Test
    fun `admin assignment replaces conflicts on both sides`() {
        val result = service.assign(principal(), USER_A_ID, PLAYER_B_ID.lowercase())

        assertThat(result.user.userId).isEqualTo(USER_A_ID)
        assertThat(result.player.playerId).isEqualTo(PLAYER_B_ID)
        assertThat(linkRepository.count()).isEqualTo(1)
        assertThat(linkRepository.findById(USER_A_ID).orElseThrow().playerId).isEqualTo(PLAYER_B_ID)
        assertThat(linkRepository.findById(USER_B_ID)).isEmpty
        assertThat(linkRepository.findByPlayerId(PLAYER_A_ID)).isNull()
        assertThat(auditRepository.findAll().map { it.action }).contains(AuditAction.ADMIN_LINK_GENTOOL_USER)
    }

    @Test
    fun `admin can search by Discord ID and unlink result`() {
        val userResult = service.searchUsers(principal(), "000002", 20)
        val playerResult = service.searchPlayers(principal(), "Player 1", 20)

        assertThat(userResult).hasSize(1)
        assertThat(userResult.single().userId).isEqualTo(USER_B_ID)
        assertThat(userResult.single().linkedPlayerId).isEqualTo(PLAYER_B_ID)
        assertThat(playerResult).hasSize(1)
        assertThat(playerResult.single().playerId).isEqualTo(PLAYER_B_ID)
        assertThat(playerResult.single().linkedUserId).isEqualTo(USER_B_ID)

        service.unlink(principal(), USER_B_ID)

        assertThat(linkRepository.findById(USER_B_ID)).isEmpty
        assertThat(auditRepository.findAll().map { it.action }).contains(AuditAction.ADMIN_UNLINK_GENTOOL_USER)
    }

    @Test
    fun `unregistered Discord user is persisted only when linked`() {
        Mockito.doReturn(
            DiscordBotUser(
                id = REMOTE_DISCORD_ID,
                displayName = "Remote Player",
                pictureUrl = "https://cdn.discordapp.com/avatars/$REMOTE_DISCORD_ID/avatar.png",
            ),
        ).`when`(discordBotClient).fetchUser(REMOTE_DISCORD_ID)

        val resolved = service.resolveDiscordUser(principal(), REMOTE_DISCORD_ID)

        assertThat(resolved.displayName).isEqualTo("Remote Player")
        assertThat(userRepository.findById("discord:$REMOTE_DISCORD_ID")).isEmpty
        assertThat(auditRepository.findAll()).isEmpty()

        val linked = service.assign(principal(), resolved.userId, PLAYER_A_ID)
        val stored = userRepository.findById(resolved.userId).orElseThrow()

        assertThat(linked.user.userId).isEqualTo(stored.userId)
        assertThat(stored.role).isEqualTo(ConsoleRole.VIEWER)
        assertThat(stored.accessStatus).isEqualTo(AccessRequestStatus.APPROVED)
        assertThat(linkRepository.findById(stored.userId).orElseThrow().playerId).isEqualTo(PLAYER_A_ID)
        assertThat(auditRepository.findAll().map { it.action })
            .contains(AuditAction.ADMIN_IMPORT_DISCORD_USER, AuditAction.ADMIN_LINK_GENTOOL_USER)
    }

    private fun user(id: String, name: String, role: ConsoleRole) = UserEntity(
        googleId = id,
        authProvider = "discord",
        displayName = name,
        role = role,
        accessStatus = AccessRequestStatus.APPROVED,
    )

    private fun principal(): AuthenticatedUserPrincipal {
        val attributes = mapOf<String, Any>("id" to ADMIN_ID.substringAfter("discord:"))
        return AuthenticatedUserPrincipal.of(
            AuthenticatedUserInfo("discord", ADMIN_ID.substringAfter("discord:"), "Admin", null),
            DefaultOAuth2User(listOf(SimpleGrantedAuthority("ROLE_USER")), attributes, "id"),
        )
    }

    private companion object {
        const val ADMIN_ID = "discord:800000000000000000"
        const val USER_A_ID = "discord:800000000000000001"
        const val USER_B_ID = "discord:800000000000000002"
        const val REMOTE_DISCORD_ID = "800000000000000003"
        const val PLAYER_A_ID = "ABCDEF123456"
        const val PLAYER_B_ID = "123456ABCDEF"
    }
}