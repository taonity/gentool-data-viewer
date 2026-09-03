package org.example.fullstackstarter.replay.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
class ReplayCollectorConfig {
    @Bean(destroyMethod = "shutdown")
    fun replayCollectorExecutor(): ExecutorService = Executors.newSingleThreadExecutor { task ->
        Thread(task, "replay-collector").apply { isDaemon = true }
    }
}