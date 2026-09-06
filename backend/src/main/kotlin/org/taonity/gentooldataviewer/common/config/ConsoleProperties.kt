package org.taonity.gentooldataviewer.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.console")
data class ConsoleProperties(
    val ownerUserIds: List<String> = emptyList(),
    val adminUserIds: List<String> = emptyList(),
) {
    fun isOwner(userId: String): Boolean =
        ownerUserIds.any { it.trim() == userId }

    fun isAdmin(userId: String): Boolean =
        adminUserIds.any { it.trim() == userId }
}
