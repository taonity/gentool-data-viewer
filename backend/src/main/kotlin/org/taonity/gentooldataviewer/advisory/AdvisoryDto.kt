package org.taonity.gentooldataviewer.advisory

data class AdvisoryDto(
    val code: String,
    val title: String,
    val detail: String,
    val severity: Severity
)
