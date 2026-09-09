package org.taonity.gentooldataviewer.security.client

import com.sun.net.httpserver.HttpServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.taonity.gentooldataviewer.security.config.DiscordProperties
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.net.InetSocketAddress
import java.net.URI
import java.nio.charset.StandardCharsets
import java.time.Duration

class DiscordBotClientTest {
    @Test
    fun `fetches Discord user with bot authorization`() {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        var authorization: String? = null
        server.createContext("/api/v10/users/$DISCORD_USER_ID") { exchange ->
            authorization = exchange.requestHeaders.getFirst("Authorization")
            val response = """
                {
                  "id": "$DISCORD_USER_ID",
                  "username": "remote_player",
                  "global_name": "Remote Player",
                  "avatar": "avatar-hash",
                  "bot": false
                }
            """.trimIndent().toByteArray(StandardCharsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        }
        server.start()

        try {
            val client = DiscordBotClient(
                DiscordProperties(
                    cdnBaseUrl = "https://cdn.discordapp.com",
                    botApiBaseUrl = URI.create("http://127.0.0.1:${server.address.port}/api/v10/"),
                    botToken = "test-token",
                    botRequestTimeout = Duration.ofSeconds(2),
                    botUserAgent = "gentool-data-viewer-test/1.0",
                ),
                jacksonObjectMapper(),
            )

            val user = client.fetchUser(DISCORD_USER_ID)

            assertThat(authorization).isEqualTo("Bot test-token")
            assertThat(user.displayName).isEqualTo("Remote Player")
            assertThat(user.pictureUrl)
                .isEqualTo("https://cdn.discordapp.com/avatars/$DISCORD_USER_ID/avatar-hash.png")
        } finally {
            server.stop(0)
        }
    }

    private companion object {
        const val DISCORD_USER_ID = "400000000000000001"
    }
}