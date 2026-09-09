package org.taonity.gentooldataviewer.replay.dto

import org.taonity.gentooldataviewer.replay.entity.GentoolLinkStatus

data class AdminDiscordUserOptionDto(
    val userId: String,
    val discordUserId: String,
    val displayName: String,
    val pictureUrl: String?,
    val linkedPlayers: List<AdminLinkedPlayerDto>,
    val maxLinkedPlayers: Int,
)

data class AdminLinkedPlayerDto(
    val playerId: String,
    val playerName: String?,
    val linkStatus: GentoolLinkStatus,
)

data class AdminGentoolPlayerOptionDto(
    val playerId: String,
    val mainName: String,
    val linkedUserId: String?,
    val linkedDiscordUserId: String?,
    val linkedDisplayName: String?,
    val linkStatus: GentoolLinkStatus?,
)

data class AdminGentoolLinkDto(
    val user: AdminDiscordUserOptionDto,
    val player: AdminGentoolPlayerOptionDto,
)

data class AdminAssignGentoolLinkBody(
    val userId: String,
    val playerId: String,
)

data class AdminResolveDiscordUserBody(
    val discordUserId: String,
)