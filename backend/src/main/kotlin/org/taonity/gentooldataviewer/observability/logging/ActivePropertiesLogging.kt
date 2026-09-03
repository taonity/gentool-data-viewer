package org.taonity.gentooldataviewer.observability.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.stereotype.Component

@Component
@Profile("active-properties-logging")
class ActivePropertiesLogging(
    private val environment: ConfigurableEnvironment
) : CommandLineRunner {

    companion object {
        private val LOGGER = KotlinLogging.logger {}
        private val SECRET_KEY_PATTERN =
            Regex("(?i)(password|passwd|pwd|secret|credential|token|apikey|api-key|private-key|authorization)")
    }

    override fun run(vararg args: String) {
        val rendered = collectProperties().entries
            .joinToString(separator = "\n") { (key, value) -> "  $key = $value" }
        LOGGER.debug { "Active application properties and environment variables (secrets masked):\n$rendered" }
    }

    private fun collectProperties(): Map<String, String> {
        val result = sortedMapOf<String, String>()
        environment.propertySources
            .filterIsInstance<EnumerablePropertySource<*>>()
            .filter { isAppOrEnvSource(it.name) }
            .forEach { source ->
                source.propertyNames.forEach { name ->
                    if (!result.containsKey(name)) {
                        environment.getProperty(name)?.let { value ->
                            result[name] = maskIfSecret(name, value)
                        }
                    }
                }
            }
        return result
    }

    private fun isAppOrEnvSource(sourceName: String): Boolean =
        sourceName == "systemEnvironment" || sourceName.contains("application", ignoreCase = true)

    private fun maskIfSecret(key: String, value: String): String =
        if (SECRET_KEY_PATTERN.containsMatchIn(key)) mask(value) else value

    private fun mask(value: String): String = when {
        value.isEmpty() -> value
        value.length > 8 -> value.take(2) + "*".repeat(value.length - 4) + value.takeLast(2)
        else -> "*".repeat(value.length)
    }
}
