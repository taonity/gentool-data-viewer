package org.taonity.gentooldataviewer.user.repository

import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, String> {
    fun findByAccessStatusOrderByDisplayNameAsc(accessStatus: AccessRequestStatus): List<UserEntity>

    fun findAllByOrderByDisplayNameAsc(): List<UserEntity>
}
