package org.taonity.gentooldataviewer.local

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.dev")
data class DevLoginProperties(
    val stubLogins: List<StubLogin> = emptyList(),
) {
    data class StubLogin(
        val registrationId: String = "",
        val label: String = "",
    )
}
