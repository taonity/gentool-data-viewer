package org.taonity.gentooldataviewer.user.repository

import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepository : JpaRepository<UserEntity, String> {
    fun findByAccessStatusOrderByDisplayNameAsc(accessStatus: AccessRequestStatus): List<UserEntity>

    fun findAllByOrderByDisplayNameAsc(): List<UserEntity>

    @Query(
        """
            SELECT u FROM UserEntity u
            WHERE u.authProvider = 'discord'
              AND (
                   :q = ''
                   OR LOWER(u.displayName) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
                   OR LOWER(u.googleId) LIKE LOWER(CASE WHEN :exact = true THEN :q ELSE CONCAT('%', :q, '%') END)
              )
            ORDER BY u.displayName, u.googleId
        """,
    )
    fun searchDiscordUsers(q: String, pageable: Pageable, exact: Boolean = false): List<UserEntity>
}
