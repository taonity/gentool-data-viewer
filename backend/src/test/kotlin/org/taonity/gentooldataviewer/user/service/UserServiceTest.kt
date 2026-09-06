package org.taonity.gentooldataviewer.user.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.taonity.gentooldataviewer.common.config.ConsoleProperties
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserInfo
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserPrincipal
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.repository.UserRepository
import java.util.Optional

class UserServiceTest {
    @Test
    fun `configured user IDs bootstrap owner and admin roles`() {
        assertThat(createdUserRole("discord:owner", ownerUserIds = listOf("discord:owner")))
            .isEqualTo(ConsoleRole.OWNER)
        assertThat(createdUserRole("discord:admin", adminUserIds = listOf("discord:admin")))
            .isEqualTo(ConsoleRole.ADMIN)
    }

    @Test
    fun `email alone does not bootstrap privileged access`() {
        assertThat(createdUserRole("discord:viewer", email = "former-owner@example.com"))
            .isEqualTo(ConsoleRole.VIEWER)
    }

    private fun createdUserRole(
        userId: String,
        email: String = "user@example.com",
        ownerUserIds: List<String> = emptyList(),
        adminUserIds: List<String> = emptyList(),
    ): ConsoleRole {
        val repository = mock(UserRepository::class.java)
        `when`(repository.findById(userId)).thenReturn(Optional.empty())
        val service = UserService(repository, ConsoleProperties(ownerUserIds, adminUserIds))
        val providerUserId = userId.substringAfter(':')
        val principal = AuthenticatedUserPrincipal(
            authorities = emptyList(),
            attributes = mapOf("id" to providerUserId),
            userInfo = AuthenticatedUserInfo("discord", providerUserId, email, "User", null),
        )

        service.createOrUpdateUser(principal)

        val captor = ArgumentCaptor.forClass(UserEntity::class.java)
        verify(repository).save(captor.capture())
        return captor.value.role
    }
}