package org.taonity.gentooldataviewer.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.taonity.gentooldataviewer.other.ControllerTestsBaseClass
import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Autowired

class DiscordOAuthLoginTest : ControllerTestsBaseClass() {
    @Autowired
    lateinit var userRepository: UserRepository

    @Test
    fun `discord login bootstraps owner from namespaced user ID`() {
        authorizeOAuth2()

        val user = userRepository.findById("discord:100000000000000000").orElseThrow()
        assertThat(user.authProvider).isEqualTo("discord")
        assertThat(user.displayName).isEqualTo("Test Owner")
        assertThat(user.role).isEqualTo(ConsoleRole.OWNER)
        assertThat(user.accessStatus).isEqualTo(AccessRequestStatus.APPROVED)
        assertThat(user.pictureUrl).isEqualTo(
            "https://cdn.discordapp.com/avatars/100000000000000000/owner-avatar.png"
        )
    }

    @Test
    fun `discord login creates viewer from identity attributes`() {
        userRepository.deleteById("discord:100000000000000001")

        authorizeOAuth2("stub-alice")

        val user = userRepository.findById("discord:100000000000000001").orElseThrow()
        assertThat(user.authProvider).isEqualTo("discord")
        assertThat(user.displayName).isEqualTo("Alice Tester")
        assertThat(user.role).isEqualTo(ConsoleRole.VIEWER)
        assertThat(user.accessStatus).isEqualTo(AccessRequestStatus.APPROVED)
    }
}