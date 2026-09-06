package org.taonity.gentooldataviewer.web.exception

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

class GlobalExceptionHandlerTest {
    @Test
    fun `too many requests remains a structured client error`() {
        val response = GlobalExceptionHandler().handleResponseStatus(
            ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "This player was rescanned recently")
        )

        assertThat(response.statusCode).isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
        assertThat(response.body).isEqualTo(
            ClientErrorResponse(ClientErrorCode.TOO_MANY_REQUESTS, "This player was rescanned recently")
        )
    }
}