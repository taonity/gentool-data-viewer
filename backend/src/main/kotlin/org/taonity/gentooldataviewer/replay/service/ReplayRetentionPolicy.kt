package org.taonity.gentooldataviewer.replay.service

import org.springframework.stereotype.Component
import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.replay.dto.ReplayCollectionWindowDto
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Component
class ReplayRetentionPolicy(private val settings: AppSettings, private val clock: Clock) {
    fun window(): ReplayCollectionWindowDto {
        val days = settings.retention().replays.retentionDays
        val today = LocalDate.now(clock.withZone(ZoneOffset.UTC))
        return ReplayCollectionWindowDto(days, today.minusDays(days - 1), today)
    }

    fun cutoff(): Instant = window().startDate.atStartOfDay().toInstant(ZoneOffset.UTC)

    fun accepts(sourceDate: LocalDate, matchAt: Instant): Boolean {
        val window = window()
        return sourceDate >= window.startDate && sourceDate <= window.endDate &&
            matchAt >= window.startDate.atStartOfDay().toInstant(ZoneOffset.UTC) && matchAt <= clock.instant()
    }
}
