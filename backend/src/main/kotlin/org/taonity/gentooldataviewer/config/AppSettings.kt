package org.taonity.gentooldataviewer.config

import io.github.oshai.kotlinlogging.KotlinLogging
import org.taonity.gentooldataviewer.config.repository.ConfigOverrideRepository
import org.taonity.gentooldataviewer.console.config.ConsolePagingProperties
import org.taonity.gentooldataviewer.replay.config.ReplayRescanProperties
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class AppSettings(
    private val retentionDefaults: RetentionProperties,
    private val consoleDefaults: ConsolePagingProperties,
    private val replayRescanDefaults: ReplayRescanProperties,
    private val registry: ConfigRegistry,
    private val overrideRepository: ConfigOverrideRepository,
) {
    private companion object {
        private val LOGGER = KotlinLogging.logger {}
    }

    private val yamlDefaults = EffectiveConfig(retentionDefaults, consoleDefaults, replayRescanDefaults)

    private val snapshot = AtomicReference(yamlDefaults)

    fun retention(): RetentionProperties = snapshot.get().retention

    fun console(): ConsolePagingProperties = snapshot.get().console

    fun replayRescan(): ReplayRescanProperties = snapshot.get().replayRescan

    /** The reset target: yaml defaults. */
    fun defaults(): EffectiveConfig = yamlDefaults

    fun effective(): EffectiveConfig = snapshot.get()

    @EventListener(ApplicationReadyEvent::class)
    fun warmUp() = reload()

    fun reload() {
        var effective = yamlDefaults
        for (row in overrideRepository.findAll()) {
            val field = registry.field(row.configKey)
            if (field == null) {
                LOGGER.warn { "Ignoring unknown config override key '${row.configKey}'" }
                continue
            }
            effective = try {
                field.apply(effective, registry.parseStored(field, row.valueJson, effective))
            } catch (e: Exception) {
                LOGGER.warn(e) { "Ignoring invalid config override '${row.configKey}'" }
                effective
            }
        }
        snapshot.set(effective)
    }
}
