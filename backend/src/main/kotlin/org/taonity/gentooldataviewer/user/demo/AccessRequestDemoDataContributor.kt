package org.taonity.gentooldataviewer.user.demo

import org.taonity.gentooldataviewer.common.demo.DemoDataContributor
import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.repository.UserRepository
import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("demo-data")
@Order(100)
class AccessRequestDemoDataContributor(
    private val userRepository: UserRepository,
) : DemoDataContributor {
    override val feature: String = "access-requests"

    @Transactional
    override fun seed(): Int {
        val users = fixtures().filterNot { userRepository.existsById(it.googleId) }
        userRepository.saveAll(users)
        return users.size
    }

    private fun fixtures() = listOf(
        UserEntity(
            googleId = "discord:200000000000000001",
            authProvider = "discord",
            email = "alice@example.com",
            displayName = "Alice Tester",
            role = ConsoleRole.NONE,
            accessStatus = AccessRequestStatus.PENDING,
            requestedRole = ConsoleRole.VIEWER,
        ),
        UserEntity(
            googleId = "discord:200000000000000002",
            authProvider = "discord",
            email = "bob@example.com",
            displayName = "Bob Tester",
            role = ConsoleRole.NONE,
            accessStatus = AccessRequestStatus.PENDING,
            requestedRole = ConsoleRole.EDITOR,
        ),
    )
}
