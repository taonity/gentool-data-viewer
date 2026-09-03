package org.taonity.gentooldataviewer.observability.logging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.core.env.Environment
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("h2", "local")
class ActivePropertiesLoggingProfileTest {

    @Autowired
    lateinit var applicationContext: ApplicationContext

    @Autowired
    lateinit var environment: Environment

    @Test
    fun `local profile activates active properties logging`() {
        assertThat(environment.activeProfiles)
            .contains("local", "plain-log", "active-properties-logging")
        assertThat(applicationContext.getBeansOfType(ActivePropertiesLogging::class.java))
            .hasSize(1)
    }
}
