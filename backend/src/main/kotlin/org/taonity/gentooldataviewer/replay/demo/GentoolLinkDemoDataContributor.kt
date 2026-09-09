package org.taonity.gentooldataviewer.replay.demo

import org.taonity.gentooldataviewer.common.demo.DemoDataContributor
import org.taonity.gentooldataviewer.replay.entity.GentoolLinkStatus
import org.taonity.gentooldataviewer.replay.entity.GentoolUserLinkEntity
import org.taonity.gentooldataviewer.replay.repository.GentoolUserLinkRepository
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.security.config.DiscordProperties
import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.repository.UserRepository
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
@Profile("demo-data")
@Order(400)
class GentoolLinkDemoDataContributor(
    private val userRepository: UserRepository,
    private val linkRepository: GentoolUserLinkRepository,
    private val playerRepository: PlayerHardwareRepository,
    private val discordProperties: DiscordProperties,
) : DemoDataContributor {
    override val feature: String = "gentool-links"

    @Transactional
    override fun seed(): Int {
        var inserted = 0
        fixtures().forEach { fixture ->
            if (!userRepository.existsById(fixture.userId)) {
                userRepository.save(
                    UserEntity(
                        googleId = fixture.userId,
                        authProvider = "discord",
                        displayName = fixture.displayName,
                        pictureUrl = avatarUrl(fixture.avatarIndex),
                        role = ConsoleRole.VIEWER,
                        accessStatus = AccessRequestStatus.APPROVED,
                    ),
                )
                inserted++
            }
            if (
                playerRepository.existsById(fixture.playerId) &&
                linkRepository.findByUserIdAndPlayerId(fixture.userId, fixture.playerId) == null &&
                linkRepository.findByPlayerId(fixture.playerId) == null
            ) {
                linkRepository.save(
                    GentoolUserLinkEntity(
                        userId = fixture.userId,
                        playerId = fixture.playerId,
                        status = GentoolLinkStatus.APPROVED,
                        requestedAt = LINKED_AT,
                        decidedAt = LINKED_AT,
                        decidedByUserId = fixture.userId,
                    ),
                )
                inserted++
            }
        }
        return inserted
    }

    private fun avatarUrl(index: Int): String =
        "${discordProperties.cdnBaseUrl.trimEnd('/')}/embed/avatars/$index.png"

    private data class LinkFixture(
        val userId: String,
        val displayName: String,
        val playerId: String,
        val avatarIndex: Int,
    )

    private companion object {
        val LINKED_AT: Instant = Instant.parse("2026-09-02T23:00:00Z")
        val FIXTURES = listOf(
            LinkFixture("discord:300000000000000001", "BirchLeaf", "D3A000000002", 0),
            LinkFixture("discord:300000000000000002", "Cobalt", "D3A000000003", 1),
            LinkFixture("discord:300000000000000003", "Delta One", "D3A000000004", 2),
            LinkFixture("discord:300000000000000004", "EmberFox", "D3A000000005", 3),
            LinkFixture("discord:300000000000000005", "FluxCap", "D3A000000006", 4),
            LinkFixture("discord:300000000000000001", "BirchLeaf", "D3A000000007", 0),
            LinkFixture("discord:300000000000000002", "Cobalt", "D3A000000008", 1),
            LinkFixture("discord:300000000000000002", "Cobalt", "D3A000000009", 1),
        )
    }

    private fun fixtures(): List<LinkFixture> = FIXTURES
}