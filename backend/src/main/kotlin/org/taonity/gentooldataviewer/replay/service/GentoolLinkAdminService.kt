package org.taonity.gentooldataviewer.replay.service

import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.console.entity.AuditAction
import org.taonity.gentooldataviewer.console.exception.ConsoleNotFoundException
import org.taonity.gentooldataviewer.console.service.AccessGuard
import org.taonity.gentooldataviewer.console.service.AuditService
import org.taonity.gentooldataviewer.replay.dto.AdminDiscordUserOptionDto
import org.taonity.gentooldataviewer.replay.dto.AdminGentoolLinkDto
import org.taonity.gentooldataviewer.replay.dto.AdminGentoolPlayerOptionDto
import org.taonity.gentooldataviewer.replay.dto.AdminLinkedPlayerDto
import org.taonity.gentooldataviewer.replay.entity.GentoolLinkStatus
import org.taonity.gentooldataviewer.replay.entity.GentoolUserLinkEntity
import org.taonity.gentooldataviewer.replay.entity.PlayerHardwareEntity
import org.taonity.gentooldataviewer.replay.repository.GentoolUserLinkRepository
import org.taonity.gentooldataviewer.replay.repository.PlayerHardwareRepository
import org.taonity.gentooldataviewer.security.principal.GoogleUserPrincipal
import org.taonity.gentooldataviewer.security.client.DiscordBotClient
import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.repository.UserRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Service
class GentoolLinkAdminService(
    private val accessGuard: AccessGuard,
    private val userRepository: UserRepository,
    private val playerRepository: PlayerHardwareRepository,
    private val linkRepository: GentoolUserLinkRepository,
    private val auditService: AuditService,
    private val settings: AppSettings,
    private val discordBotClient: DiscordBotClient,
) {
    @Transactional(readOnly = true)
    fun searchUsers(principal: GoogleUserPrincipal, query: String?, size: Int): List<AdminDiscordUserOptionDto> {
        accessGuard.requireAdmin(principal)
        val users = userRepository.searchDiscordUsers(
            query?.trim().orEmpty(),
            PageRequest.of(0, size.coerceIn(1, settings.console().maxPageSize)),
        )
        val links = linkRepository.findByUserIdIn(users.map { it.userId })
        val linksByUserId = links.groupBy { it.userId }
        val players = playerRepository.findAllById(links.map { it.playerId }).associateBy { it.playerId }
        return users.map { user -> userOption(user, linksByUserId[user.userId].orEmpty(), players) }
    }

    @Transactional(readOnly = true)
    fun searchPlayers(principal: GoogleUserPrincipal, query: String?, size: Int): List<AdminGentoolPlayerOptionDto> {
        accessGuard.requireAdmin(principal)
        val players = playerRepository.search(
            query?.trim().orEmpty(),
            "all",
            PageRequest.of(
                0,
                size.coerceIn(1, settings.console().maxPageSize),
                Sort.by(Sort.Order.asc("mainName"), Sort.Order.asc("playerId")),
            ),
        ).content
        val links = linkRepository.findByPlayerIdIn(players.map { it.playerId }).associateBy { it.playerId }
        val users = userRepository.findAllById(links.values.map { it.userId }).associateBy { it.userId }
        return players.map { player -> playerOption(player, links[player.playerId], users) }
    }

    @Transactional
    fun resolveDiscordUser(
        principal: GoogleUserPrincipal,
        rawDiscordUserId: String,
    ): AdminDiscordUserOptionDto {
        accessGuard.requireAdmin(principal)
        val discordUserId = rawDiscordUserId.trim()
        if (!DISCORD_USER_ID.matches(discordUserId)) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Discord user ID must contain 17 to 20 digits")
        }
        val fetched = discordBotClient.fetchUser(discordUserId)
        val userId = "discord:${fetched.id}"
        val existing = userRepository.findById(userId).orElse(null)
        if (existing != null && existing.authProvider != "discord") {
            throw ResponseStatusException(HttpStatus.CONFLICT, "Discord identity conflicts with an existing account")
        }
        val preview = UserEntity(
            googleId = userId,
            authProvider = "discord",
            displayName = fetched.displayName,
            pictureUrl = fetched.pictureUrl,
            role = existing?.role ?: ConsoleRole.VIEWER,
            accessStatus = existing?.accessStatus ?: AccessRequestStatus.APPROVED,
        )
        val links = linkRepository.findByUserId(userId)
        val players = playerRepository.findAllById(links.map { it.playerId }).associateBy { it.playerId }
        return userOption(preview, links, players)
    }

    @Transactional
    fun assign(
        principal: GoogleUserPrincipal,
        userId: String,
        rawPlayerId: String,
    ): AdminGentoolLinkDto {
        val actor = accessGuard.requireAdmin(principal)
        val playerId = rawPlayerId.trim().uppercase()
        val player = playerRepository.findById(playerId)
            .orElseThrow { ConsoleNotFoundException("GenTool player not found") }
        val existingUser = userRepository.findById(userId).orElse(null)
        if (existingUser != null && existingUser.authProvider != "discord") {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Selected account is not a Discord user")
        }
        val user = existingUser ?: fetchUnregisteredUser(userId)
        if (existingUser == null) {
            userRepository.save(user)
            auditService.record(AuditAction.ADMIN_IMPORT_DISCORD_USER, "user", user.userId, actor)
        }
        val userLinks = linkRepository.findByUserId(user.userId)
        val existingLink = linkRepository.findByUserIdAndPlayerId(user.userId, player.playerId)
        val playerLink = linkRepository.findByPlayerId(player.playerId)
        val now = Instant.now()

        if (existingLink == null && userLinks.size >= settings.replayRescan().maxLinkedPlayers) {
            throw ResponseStatusException(
                HttpStatus.CONFLICT,
                "A Discord account can link at most ${settings.replayRescan().maxLinkedPlayers} GenTool players",
            )
        }

        if (playerLink != null && playerLink.userId != user.userId) {
            linkRepository.delete(playerLink)
            linkRepository.flush()
        }
        val assigned = existingLink?.apply {
            status = GentoolLinkStatus.APPROVED
            requestedAt = now
            decidedAt = now
            decidedByUserId = actor.userId
        } ?: GentoolUserLinkEntity(
            userId = user.userId,
            playerId = player.playerId,
            status = GentoolLinkStatus.APPROVED,
            requestedAt = now,
            decidedAt = now,
            decidedByUserId = actor.userId,
        )
        linkRepository.save(assigned)
        auditService.record(AuditAction.ADMIN_LINK_GENTOOL_USER, "gentool_player", player.playerId, actor)
        val updatedLinks = userLinks.filterNot { it.playerId == assigned.playerId } + assigned
        return AdminGentoolLinkDto(
            userOption(
                user,
                updatedLinks,
                playerRepository.findAllById(updatedLinks.map { it.playerId }).associateBy { it.playerId },
            ),
            playerOption(player, assigned, mapOf(user.userId to user)),
        )
    }

    @Transactional
    fun unlink(principal: GoogleUserPrincipal, userId: String, playerId: String) {
        val actor = accessGuard.requireAdmin(principal)
        val link = linkRepository.findByUserIdAndPlayerId(userId, playerId)
            ?: throw ConsoleNotFoundException("GenTool link not found")
        linkRepository.delete(link)
        auditService.record(AuditAction.ADMIN_UNLINK_GENTOOL_USER, "gentool_player", link.playerId, actor)
    }

    private fun userOption(
        user: UserEntity,
        links: List<GentoolUserLinkEntity>,
        players: Map<String, PlayerHardwareEntity>,
    ) = AdminDiscordUserOptionDto(
        userId = user.userId,
        discordUserId = user.userId.substringAfter("discord:"),
        displayName = user.displayName,
        pictureUrl = user.pictureUrl,
        linkedPlayers = links.map { link ->
            AdminLinkedPlayerDto(link.playerId, players[link.playerId]?.mainName, link.status)
        }.sortedBy { it.playerName ?: it.playerId },
        maxLinkedPlayers = settings.replayRescan().maxLinkedPlayers,
    )

    private fun playerOption(
        player: PlayerHardwareEntity,
        link: GentoolUserLinkEntity?,
        users: Map<String, UserEntity>,
    ): AdminGentoolPlayerOptionDto {
        val user = link?.let { users[it.userId] }
        return AdminGentoolPlayerOptionDto(
            playerId = player.playerId,
            mainName = player.mainName,
            linkedUserId = link?.userId,
            linkedDiscordUserId = user?.userId?.substringAfter("discord:"),
            linkedDisplayName = user?.displayName,
            linkStatus = link?.status,
        )
    }

    private fun fetchUnregisteredUser(userId: String): UserEntity {
        val discordUserId = userId.takeIf { it.startsWith("discord:") }?.substringAfter("discord:")
        if (discordUserId == null || !DISCORD_USER_ID.matches(discordUserId)) {
            throw ConsoleNotFoundException("Discord user not found")
        }
        val fetched = discordBotClient.fetchUser(discordUserId)
        return UserEntity(
            googleId = userId,
            authProvider = "discord",
            displayName = fetched.displayName,
            pictureUrl = fetched.pictureUrl,
            role = ConsoleRole.VIEWER,
            accessStatus = AccessRequestStatus.APPROVED,
        )
    }

    private companion object {
        val DISCORD_USER_ID = Regex("[0-9]{17,20}")
    }
}