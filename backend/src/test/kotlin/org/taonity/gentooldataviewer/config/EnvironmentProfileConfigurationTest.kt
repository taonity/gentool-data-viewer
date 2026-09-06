package org.taonity.gentooldataviewer.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class EnvironmentProfileConfigurationTest {

    @Test
    fun `stub Discord profile bootstraps owner by user ID`() {
        ApplicationContextRunner()
            .withInitializer(ConfigDataApplicationContextInitializer())
            .withSystemProperties("spring.profiles.active=stub-discord")
            .run { context ->
                assertThat(context.environment.getProperty("app.console.owner-discord-ids"))
                    .isEqualTo("100000000000000000")
            }
    }

    @Test
    fun `stage profile loads its public deployment configuration`() {
        assertEnvironmentProfile(
            profile = "stage",
            frontendUrl = "https://gentool-data-viewer-stage.taonity.org",
            sessionCookieName = "JSESSIONID-GENTOOL-DATA-VIEWER-STAGE",
            csrfCookieName = "XSRF-TOKEN-GENTOOL-DATA-VIEWER-STAGE",
            scheduledCron = "0 15 6 * * *",
            scheduledUserLimit = "20",
        )
    }

    @Test
    fun `prod profile loads its public deployment configuration`() {
        assertEnvironmentProfile(
            profile = "prod",
            frontendUrl = "https://gentool-data-viewer.taonity.org",
            sessionCookieName = "JSESSIONID-GENTOOL-DATA-VIEWER-PROD",
            csrfCookieName = "XSRF-TOKEN-GENTOOL-DATA-VIEWER-PROD",
            scheduledCron = "0 15 2 * * *",
            scheduledUserLimit = null,
        )
    }

    private fun assertEnvironmentProfile(
        profile: String,
        frontendUrl: String,
        sessionCookieName: String,
        csrfCookieName: String,
        scheduledCron: String,
        scheduledUserLimit: String?,
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
                assertThat(
                    environment.getProperty(
                        "spring.security.oauth2.client.registration.discord-gentool-data-viewer.redirect-uri"
                    )
                ).isEqualTo("{baseUrl}/login/oauth2/code/{registrationId}")
                assertThat(environment.getProperty("app.replay-collector.schedule-cron"))
                    .isEqualTo(scheduledCron)
                assertThat(environment.getProperty("app.replay-collector.schedule-user-limit"))
                    .isEqualTo(scheduledUserLimit)
                assertThat(environment.getProperty("spring.datasource.url"))
                    .isEqualTo("jdbc:postgresql://db:5432/gentool_data_viewer_db")
                assertThat(environment.getProperty("spring.security.oauth2.client.provider.discord.authorization-uri"))
                    .isEqualTo("https://discord.com/oauth2/authorize")
            }
    }
}