package org.taonity.gentooldataviewer.security.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.taonity.gentooldataviewer.security.config.DiscordProperties
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserInfo
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserPrincipal
import org.taonity.gentooldataviewer.user.service.UserService
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service

@Service
class OAuth2UserPersistenceService(
    private val userService: UserService,
    private val discordProperties: DiscordProperties,
) : DefaultOAuth2UserService() {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    override fun loadUser(userRequest: OAuth2UserRequest?): OAuth2User {
        val validatedUserRequest = requireNotNull(userRequest) { "OAuth2UserRequest must not be null" }

        val oAuth2User: OAuth2User = try {
            super.loadUser(validatedUserRequest)
        } catch (e: OAuth2AuthenticationException) {
            LOGGER.error(e) { "OAuth2 user loading failed" }
            throw e
        }

        val userInfo = toUserInfo(oAuth2User.attributes)
        val principal = AuthenticatedUserPrincipal.of(userInfo, oAuth2User)
        userService.createOrUpdateUser(principal)
        return principal
    }

    private fun toUserInfo(attributes: Map<String, Any>): AuthenticatedUserInfo {
        val id = attributes["id"] as? String ?: invalidUserInfo("Discord user ID is missing")
        val email = attributes["email"] as? String ?: invalidUserInfo("Discord user email is missing")
        if (attributes["verified"] != true) invalidUserInfo("Discord user email is not verified")
        val avatarHash = attributes["avatar"] as? String
        return AuthenticatedUserInfo(
            provider = "discord",
            id = id,
            email = email,
            displayName = (attributes["global_name"] as? String)
                ?: (attributes["username"] as? String)
                ?: email,
            pictureUrl = avatarHash?.let {
                "${discordProperties.cdnBaseUrl.trimEnd('/')}/avatars/$id/$it.png"
            },
        )
    }

    private fun invalidUserInfo(message: String): Nothing =
        throw OAuth2AuthenticationException(OAuth2Error("invalid_user_info_response"), message)
}
