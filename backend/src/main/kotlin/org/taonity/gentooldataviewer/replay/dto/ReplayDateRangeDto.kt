package org.taonity.gentooldataviewer.replay.dto

import java.time.LocalDate

data class ReplayDateRangeDto(
    val startDate: LocalDate?,
    val endDate: LocalDate?,
)