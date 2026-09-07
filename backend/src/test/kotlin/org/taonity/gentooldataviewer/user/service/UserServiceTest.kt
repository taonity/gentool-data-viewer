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
        val owner = createdUser("100000000000000001", ownerUserIds = listOf("100000000000000001"))
        val admin = createdUser("100000000000000002", adminUserIds = listOf("100000000000000002"))

        assertThat(owner.googleId).isEqualTo("discord:100000000000000001")
        assertThat(owner.role).isEqualTo(ConsoleRole.OWNER)
        assertThat(admin.googleId).isEqualTo("discord:100000000000000002")
        assertThat(admin.role).isEqualTo(ConsoleRole.ADMIN)
    }

    @Test
    fun `unconfigured user ID does not bootstrap privileged access`() {
        assertThat(createdUser("100000000000000003").role)
            .isEqualTo(ConsoleRole.VIEWER)
    }

    private fun createdUser(
        providerUserId: String,
        ownerUserIds: List<String> = emptyList(),
        adminUserIds: List<String> = emptyList(),
    ): UserEntity {
        val repository = mock(UserRepository::class.java)
        `when`(repository.findById("discord:$providerUserId")).thenReturn(Optional.empty())
        val service = UserService(repository, ConsoleProperties(ownerUserIds, adminUserIds))
        val principal = AuthenticatedUserPrincipal(
            authorities = emptyList(),
            attributes = mapOf("id" to providerUserId),
            userInfo = AuthenticatedUserInfo("discord", providerUserId, "User", null),
        )

        service.createOrUpdateUser(principal)

        val captor = ArgumentCaptor.forClass(UserEntity::class.java)
        verify(repository).save(captor.capture())
        return captor.value
    }
}