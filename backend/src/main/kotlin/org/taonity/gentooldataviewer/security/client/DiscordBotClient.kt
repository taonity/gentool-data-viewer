package org.taonity.gentooldataviewer.security.client

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import org.taonity.gentooldataviewer.security.config.DiscordProperties
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.io.IOException

data class DiscordBotUser(
    val id: String,
    val displayName: String,
    val pictureUrl: String?,
)

@Component
class DiscordBotClient(
    private val properties: DiscordProperties,
    private val objectMapper: ObjectMapper,
) {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(properties.botRequestTimeout)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    fun fetchUser(discordUserId: String): DiscordBotUser {
        if (properties.botToken.isBlank()) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Discord bot integration is not configured")
        }
        val uri = botUserUri(discordUserId)
        val request = HttpRequest.newBuilder(uri)
            .timeout(properties.botRequestTimeout)
            .header("Accept", "application/json")
            .header("Authorization", "Bot ${properties.botToken}")
            .header("User-Agent", properties.botUserAgent)
            .GET()
            .build()
        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Discord lookup was interrupted", e)
        } catch (e: IOException) {
            throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Discord lookup failed", e)
        }
        when (response.statusCode()) {
            200 -> Unit
            404 -> throw ResponseStatusException(HttpStatus.NOT_FOUND, "Discord user not found")
            401, 403 -> throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Discord bot authentication failed")
            429 -> throw ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Discord lookup is rate limited")
            else -> throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Discord lookup failed")
        }

        val body = try {
            objectMapper.readTree(response.body())
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Discord returned an invalid user response", e)
        }
        val id = body.get("id")?.stringValue()?.takeIf(String::isNotBlank)
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Discord returned an invalid user response")
        val username = body.get("username")?.stringValue()?.takeIf(String::isNotBlank)
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Discord returned an invalid user response")
        if (id != discordUserId) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "Discord returned an unexpected user")
        }
        if (body.get("bot")?.asBoolean(false) == true) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Discord bots cannot be linked as players")
        }
        val displayName = body.get("global_name")?.takeUnless { it.isNull }?.stringValue()
            ?.takeIf(String::isNotBlank)
            ?: username
        val avatar = body.get("avatar")?.takeUnless { it.isNull }?.stringValue()?.takeIf(String::isNotBlank)
        return DiscordBotUser(
            id = id,
            displayName = displayName,
            pictureUrl = avatar?.let { "${properties.cdnBaseUrl.trimEnd('/')}/avatars/$id/$it.png" },
        )
    }

    private fun botUserUri(discordUserId: String): URI =
        URI.create("${properties.botApiBaseUrl.toString().trimEnd('/')}/users/$discordUserId")
}