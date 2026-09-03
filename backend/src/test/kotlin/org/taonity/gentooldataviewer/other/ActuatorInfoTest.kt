package org.taonity.gentooldataviewer.other

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest(properties = ["management.endpoints.web.exposure.include=info"])
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class ActuatorInfoTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Test
    fun `exposes concise public release information`() {
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.app.name").value("backend"))
            .andExpect(jsonPath("$.app.version").isString)
            .andExpect(jsonPath("$.git.branch").isString)
            .andExpect(jsonPath("$.git.commit").isString)
            .andExpect(jsonPath("$.build.time").isString)
            .andExpect(jsonPath("$.runtime.uptime").isNumber)
            .andExpect(jsonPath("$.build.group").doesNotExist())
            .andExpect(jsonPath("$.build.artifact").doesNotExist())
    }
}
