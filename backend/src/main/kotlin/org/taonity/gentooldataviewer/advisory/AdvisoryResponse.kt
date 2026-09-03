package org.taonity.gentooldataviewer.advisory

data class AdvisoryResponse(
    val advisories: Set<AdvisoryDto> = setOf()
)
