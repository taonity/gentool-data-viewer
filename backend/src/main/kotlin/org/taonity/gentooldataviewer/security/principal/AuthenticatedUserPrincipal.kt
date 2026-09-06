package org.taonity.gentooldataviewer.security.principal

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.oidc.OidcIdToken
import org.springframework.security.oauth2.core.oidc.OidcUserInfo
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import java.io.Serializable

class AuthenticatedUserPrincipal(
    private val authorities: Collection<GrantedAuthority>,
    private val attributes: Map<String, Any>,
    val userInfo: AuthenticatedUserInfo,
    private val idToken: OidcIdToken? = null,
    private val oidcUserInfo: OidcUserInfo? = null,
) : OidcUser, Serializable {
    override fun getName(): String = getUserId()

    override fun getAttributes(): Map<String, Any> = attributes

    override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getIdToken(): OidcIdToken =
        idToken ?: OidcIdToken("empty", null, null, mapOf("sub" to userInfo.id))

    override fun getUserInfo(): OidcUserInfo? = oidcUserInfo

    override fun getClaims(): Map<String, Any> = attributes

    fun getUserId(): String = "${userInfo.provider}:${userInfo.id}"

    fun getDiscordUserId(): String = userInfo.id

    override fun getEmail(): String = userInfo.email

    fun getDisplayName(): String = userInfo.displayName

    fun getPictureUrl(): String? = userInfo.pictureUrl

    companion object {
        private const val serialVersionUID: Long = 1L

        fun of(userInfo: AuthenticatedUserInfo, oAuth2User: OAuth2User): AuthenticatedUserPrincipal =
            AuthenticatedUserPrincipal(
                authorities = oAuth2User.authorities,
                attributes = oAuth2User.attributes,
                userInfo = userInfo,
                idToken = (oAuth2User as? OidcUser)?.idToken,
                oidcUserInfo = (oAuth2User as? OidcUser)?.userInfo,
            )
    }
}