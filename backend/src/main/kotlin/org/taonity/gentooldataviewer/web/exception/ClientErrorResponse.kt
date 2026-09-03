package org.taonity.gentooldataviewer.web.exception

data class ClientErrorResponse(
    val clientErrorCode: ClientErrorCode,
    val errorMessage: String
)
