package org.taonity.gentooldataviewer.console.dto

import org.taonity.gentooldataviewer.user.entity.AccessRequestStatus
import org.taonity.gentooldataviewer.user.entity.ConsoleRole

data class AccessInfoResponse(
    val email: String,
    val displayName: String,
    val role: ConsoleRole,
    val accessStatus: AccessRequestStatus,
    val requestedRole: ConsoleRole?,
    val canView: Boolean,
    val canEdit: Boolean,
    val isAdmin: Boolean,
    val isOwner: Boolean,
)

data class AccessRequestBody(
    val requestedRole: ConsoleRole,
)

data class ApproveAccessBody(
    val role: ConsoleRole,
)

data class PendingRequestDto(
    val googleId: String,
    val email: String,
    val displayName: String,
    val requestedRole: ConsoleRole?,
)

data class UserSummaryDto(
    val googleId: String,
    val email: String,
    val displayName: String,
    val role: ConsoleRole,
    val accessStatus: AccessRequestStatus,
    val requestedRole: ConsoleRole?,
)

data class ChangeRoleBody(
    val role: ConsoleRole,
)
