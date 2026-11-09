package org.taonity.gentooldataviewer.local

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import javax.sql.DataSource

@Configuration
@Profile("demo")
class ScreenshotDemoConfig {
    @Bean
    fun screenshotClock(dataSource: DataSource, @Value("\${app.demo.instant}") instant: String): Clock {
        dataSource.connection.use { connection ->
            check(connection.metaData.url.startsWith("jdbc:h2:mem:gentool-demo")) {
                "Screenshot demo requires its isolated in-memory database"
            }
        }
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC)
    }
}
