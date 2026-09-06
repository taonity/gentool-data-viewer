package org.taonity.gentooldataviewer.other

import jakarta.servlet.http.Cookie
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.MvcResult
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.util.UriComponentsBuilder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2", "stub-discord")
open class ControllerTestsBaseClass {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Value("\${server.servlet.session.cookie.name}")
    lateinit var sessionCookieName: String

    fun authorizeOAuth2(registrationId: String = "discord-gentool-data-viewer"): Cookie {
        val authResult = mockMvc.perform(
            get("/oauth2/authorization/$registrationId")
        )
            .andExpect(status().is3xxRedirection)
            .andReturn()
        val state = getState(authResult)
        val authRequestCookie = extractSessionCookie(authResult)
            ?: error("No '$sessionCookieName' session cookie set on the authorization request response")

        val callbackResult = mockMvc.perform(
            get("/login/oauth2/code/$registrationId")
                .cookie(authRequestCookie)
                .param("code", "stub-auth-code")
                .param("state", state)
        )
            .andExpect(status().is3xxRedirection)
            .andReturn()

        return extractSessionCookie(callbackResult) ?: authRequestCookie
    }

    private fun extractSessionCookie(result: MvcResult): Cookie? {
        return result.response.getHeaders("Set-Cookie")
            .firstOrNull { it.startsWith("$sessionCookieName=") }
            ?.substringAfter("=")
            ?.substringBefore(";")
            ?.takeIf { it.isNotEmpty() }
            ?.let { Cookie(sessionCookieName, it) }
    }

    private fun getState(authenticationMvcResult: MvcResult): String {
        val location = authenticationMvcResult.response.getHeader("Location")
        val rawState = UriComponentsBuilder.fromUriString(location!!)
            .build()
            .queryParams
            .getFirst("state")
        return URLDecoder.decode(rawState, StandardCharsets.UTF_8)
    }
}
