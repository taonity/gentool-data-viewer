package org.taonity.gentooldataviewer.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.session.SessionRepository
import org.springframework.session.jdbc.JdbcIndexedSessionRepository
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.taonity.gentooldataviewer.other.ControllerTestsBaseClass

class SessionPersistenceTest : ControllerTestsBaseClass() {

    @Autowired
    lateinit var sessionRepository: SessionRepository<*>

    @Test
    fun `session repository is jdbc-backed`() {
        assertThat(sessionRepository).isInstanceOf(JdbcIndexedSessionRepository::class.java)
    }

    @Test
    fun `oauth login can be restored from its persisted session cookie`() {
        val cookie = authorizeOAuth2()
        mockMvc.perform(get("/console/access/me").cookie(cookie))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value("discord:100000000000000000"))
    }
}
