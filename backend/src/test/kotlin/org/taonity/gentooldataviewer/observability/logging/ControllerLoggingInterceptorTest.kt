package org.taonity.gentooldataviewer.observability.logging

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.test.system.CapturedOutput
import org.springframework.boot.test.system.OutputCaptureExtension
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.method.HandlerMethod

@ExtendWith(OutputCaptureExtension::class)
class ControllerLoggingInterceptorTest {
    class TestController {
        fun list() = Unit
    }

    @Test
    fun `handled server errors are logged as failures even without an exception`(output: CapturedOutput) {
        val interceptor = ControllerLoggingInterceptor()
        val request = MockHttpServletRequest("GET", "/console/replays")
        val response = MockHttpServletResponse()
        val handler = HandlerMethod(TestController(), TestController::class.java.getMethod("list"))
        interceptor.preHandle(request, response, handler)
        response.status = 500

        interceptor.afterCompletion(request, response, handler, null)

        assertThat(output.all).contains("failed with HTTP 500").doesNotContain("completed in")
    }
}
