package org.taonity.gentooldataviewer.security.principal

import java.io.Serializable

data class AuthenticatedUserInfo(
    val provider: String,
    val id: String,
    val displayName: String,
    val pictureUrl: String?,
) : Serializable {
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}