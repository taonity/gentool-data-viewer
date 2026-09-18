package org.taonity.gentooldataviewer.replay.dto

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class MatchDatePeriod(startDate: LocalDate?, endDate: LocalDate?) {
    init {
        if (startDate != null && endDate != null && startDate > endDate) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date must not be after end date")
        }
    }

    val from: Instant? = startDate?.atStartOfDay()?.toInstant(ZoneOffset.UTC)
    val until: Instant? = endDate?.plusDays(1)?.atStartOfDay()?.toInstant(ZoneOffset.UTC)
    val active: Boolean = startDate != null || endDate != null
}