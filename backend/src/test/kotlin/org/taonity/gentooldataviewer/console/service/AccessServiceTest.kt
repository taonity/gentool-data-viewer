package org.taonity.gentooldataviewer.console.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.console.config.ConsolePagingProperties
import org.taonity.gentooldataviewer.console.entity.AuditAction
import org.taonity.gentooldataviewer.console.exception.ConsoleForbiddenException
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserInfo
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserPrincipal
import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.repository.UserRepository
import java.util.Optional

class AccessServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val auditService = mock(AuditService::class.java)
    private val settings = mock(AppSettings::class.java)
    private val service = AccessService(userRepository, AccessGuard(userRepository), auditService, settings)
    private val user = UserEntity(
        googleId = USER_ID,
        authProvider = "discord",
        displayName = "Viewer",
        role = ConsoleRole.VIEWER,
        accessStatus = AccessRequestStatus.APPROVED,
    )

    @Test
    fun `disabled flag rejects requests without changing user`() {
        `when`(settings.console()).thenReturn(consoleSettings(enabled = false))

        assertThatThrownBy { service.requestAccess(principal(), ConsoleRole.EDITOR) }
            .isInstanceOf(ConsoleForbiddenException::class.java)
            .hasMessage("Access requests are disabled")

        assertThat(user.accessStatus).isEqualTo(AccessRequestStatus.APPROVED)
        assertThat(user.requestedRole).isNull()
        verifyNoInteractions(auditService)
    }

    @Test
    fun `enabled flag creates request and is exposed to client`() {
        `when`(settings.console()).thenReturn(consoleSettings(enabled = true))
        `when`(userRepository.findById(USER_ID)).thenReturn(Optional.of(user))

        val response = service.requestAccess(principal(), ConsoleRole.EDITOR)

        assertThat(response.accessRequestsEnabled).isTrue()
        assertThat(response.accessStatus).isEqualTo(AccessRequestStatus.PENDING)
        assertThat(response.requestedRole).isEqualTo(ConsoleRole.EDITOR)
        verify(auditService).record(AuditAction.REQUEST_ACCESS, "access_request", USER_ID, user)
    }

    private fun consoleSettings(enabled: Boolean) = ConsolePagingProperties(
        maxPageSize = 100,
        accessRequestsEnabled = enabled,
    )

    private fun principal() = AuthenticatedUserPrincipal(
        authorities = emptyList(),
        attributes = mapOf("id" to USER_ID.substringAfter("discord:")),
        userInfo = AuthenticatedUserInfo(
            "discord",
            USER_ID.substringAfter("discord:"),
            "Viewer",
            null,
        ),
    )

    private companion object {
        const val USER_ID = "discord:700000000000000001"
    }
}