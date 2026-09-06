package org.taonity.gentooldataviewer.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.console")
data class ConsoleProperties(
    val ownerDiscordIds: List<String> = emptyList(),
    val adminDiscordIds: List<String> = emptyList(),
) {
    fun isOwner(discordUserId: String): Boolean =
        ownerDiscordIds.any { it.trim() == discordUserId }

    fun isAdmin(discordUserId: String): Boolean =
        adminDiscordIds.any { it.trim() == discordUserId }
}
