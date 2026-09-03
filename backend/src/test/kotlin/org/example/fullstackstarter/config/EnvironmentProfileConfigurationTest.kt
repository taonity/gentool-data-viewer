package org.example.fullstackstarter.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class EnvironmentProfileConfigurationTest {

    @Test
    fun `stage profile loads its public deployment configuration`() {
        assertEnvironmentProfile(
            profile = "stage",
            frontendUrl = "https://fullstack-starter-stage.taonity.org",
            sessionCookieName = "JSESSIONID-FULLSTACK-STARTER-STAGE",
            csrfCookieName = "XSRF-TOKEN-FULLSTACK-STARTER-STAGE",
        )
    }

    @Test
    fun `prod profile loads its public deployment configuration`() {
        assertEnvironmentProfile(
            profile = "prod",
            frontendUrl = "https://fullstack-starter.taonity.org",
            sessionCookieName = "JSESSIONID-FULLSTACK-STARTER-PROD",
            csrfCookieName = "XSRF-TOKEN-FULLSTACK-STARTER-PROD",
        )
    }

    private fun assertEnvironmentProfile(
        profile: String,
        frontendUrl: String,
        sessionCookieName: String,
        csrfCookieName: String,
    ) {
        ApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withSystemProperties("spring.profiles.active=$profile")
            .run { context ->
                val environment = context.environment

                assertThat(environment.getProperty("app.default-success-url")).isEqualTo(frontendUrl)
                assertThat(environment.getProperty("app.login-url")).isEqualTo(frontendUrl)
                assertThat(environment.getProperty("server.servlet.session.cookie.domain"))
                    .isEqualTo("taonity.org")
                assertThat(environment.getProperty("server.servlet.session.cookie.name"))
                    .isEqualTo(sessionCookieName)
                assertThat(environment.getProperty("app.csrf-cookie-name")).isEqualTo(csrfCookieName)
                assertThat(environment.getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:postgresql://db:5432/fullstack_starter_db")
                assertThat(environment.getProperty("spring.security.oauth2.client.provider.google.authorization-uri"))
                    .isEqualTo("https://accounts.google.com/o/oauth2/v2/auth")
            }
    }
}