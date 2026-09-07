package org.taonity.gentooldataviewer.user.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "app_user")
class UserEntity(
    @Id
    @Column(name = "user_id")
    val googleId: String,
    @Column(name = "auth_provider", nullable = false)
    val authProvider: String,
    var displayName: String,
    var pictureUrl: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    var role: ConsoleRole = ConsoleRole.NONE,
    @Enumerated(EnumType.STRING)
    @Column(name = "access_status", nullable = false)
    var accessStatus: AccessRequestStatus = AccessRequestStatus.NONE,
    @Enumerated(EnumType.STRING)
    @Column(name = "requested_role")
    var requestedRole: ConsoleRole? = null,
) {
    val userId: String
        get() = googleId

    fun updateDetails(displayName: String, pictureUrl: String?): UserEntity {
        this.displayName = displayName
        this.pictureUrl = pictureUrl
        return this
    }

    fun grantAdmin(): UserEntity {
        this.role = ConsoleRole.ADMIN
        this.accessStatus = AccessRequestStatus.APPROVED
        this.requestedRole = null
        return this
    }

    fun grantOwner(): UserEntity {
        this.role = ConsoleRole.OWNER
        this.accessStatus = AccessRequestStatus.APPROVED
        this.requestedRole = null
        return this
    }

    override fun hashCode(): Int = googleId.hashCode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as UserEntity
        return googleId == other.googleId
    }

    override fun toString(): String {
        return "UserEntity(googleId='$googleId', displayName='$displayName', role=$role)"
    }
}
