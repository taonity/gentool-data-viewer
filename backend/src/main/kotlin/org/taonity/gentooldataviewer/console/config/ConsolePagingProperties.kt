package org.taonity.gentooldataviewer.console.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.console")
data class ConsolePagingProperties(
    val maxPageSize: Int,
    val accessRequestsEnabled: Boolean = false,
)
