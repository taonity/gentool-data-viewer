package org.taonity.gentooldataviewer.security

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class PublicReadSecurityTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `analytics reads are public`() {
        listOf(
            "/console/replays",
            "/console/cpu-players",
            "/console/cpu-players/summary"
        ).forEach { path ->
            mockMvc.perform(get(path))
                .andExpect(status().isOk)
        }
    }

    @Test
    fun `management reads require authentication`() {
        listOf(
            "/console/replay-collection/jobs",
            "/console/config",
            "/console/users"
        ).forEach { path ->
            mockMvc.perform(get(path))
                .andExpect(status().isUnauthorized)
        }
    }
}