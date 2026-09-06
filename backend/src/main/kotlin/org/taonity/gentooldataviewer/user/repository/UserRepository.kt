package org.taonity.gentooldataviewer.user.repository

import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, String> {
    fun findFirstByEmailIgnoreCase(email: String): UserEntity?

    fun findByAccessStatusOrderByEmailAsc(accessStatus: AccessRequestStatus): List<UserEntity>

    fun findAllByOrderByEmailAsc(): List<UserEntity>
}
