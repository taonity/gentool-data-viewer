package org.taonity.gentooldataviewer.config

import org.taonity.gentooldataviewer.console.service.AuditLogCleanupService
import org.taonity.gentooldataviewer.replay.config.ReplayCollectorProperties
import org.taonity.gentooldataviewer.replay.service.ReplayCollectionCoordinator
import org.taonity.gentooldataviewer.cpu.config.CpuBenchmarkProperties
import org.taonity.gentooldataviewer.cpu.service.CpuBenchmarkSyncService
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.Trigger
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.SchedulingConfigurer
import org.springframework.scheduling.config.ScheduledTaskRegistrar
import org.springframework.scheduling.support.CronTrigger
import java.time.ZoneId

@Configuration
@EnableScheduling
class DynamicSchedulingConfig(
    private val settings: AppSettings,
    private val auditLogCleanupService: AuditLogCleanupService,
    private val replayCollectorProperties: ReplayCollectorProperties,
    private val replayCollectionCoordinator: ReplayCollectionCoordinator,
    private val cpuBenchmarkProperties: CpuBenchmarkProperties,
    private val cpuBenchmarkSyncService: CpuBenchmarkSyncService,
) : SchedulingConfigurer {
    override fun configureTasks(taskRegistrar: ScheduledTaskRegistrar) {
        taskRegistrar.addTriggerTask(
            { auditLogCleanupService.cleanupOldAuditLogs() },
            Trigger { ctx -> CronTrigger(settings.retention().audit.cron).nextExecution(ctx) },
        )
        if (replayCollectorProperties.scheduleEnabled) {
            taskRegistrar.addTriggerTask(
                { replayCollectionCoordinator.startScheduledYesterday() },
                Trigger { ctx ->
                    CronTrigger(
                        replayCollectorProperties.scheduleCron,
                        ZoneId.of(replayCollectorProperties.scheduleZone),
                    ).nextExecution(ctx)
                },
            )
        }
        if (cpuBenchmarkProperties.scheduleEnabled) {
            taskRegistrar.addTriggerTask(
                { cpuBenchmarkSyncService.scheduledSync() },
                Trigger { ctx ->
                    CronTrigger(
                        cpuBenchmarkProperties.scheduleCron,
                        ZoneId.of(cpuBenchmarkProperties.scheduleZone),
                    ).nextExecution(ctx)
                },
            )
        }
    }
}
