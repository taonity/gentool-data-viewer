package org.taonity.gentooldataviewer.replay.client

import io.github.oshai.kotlinlogging.KotlinLogging
import org.taonity.gentooldataviewer.replay.config.ReplayCollectorProperties
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Component
class GentoolSourceClient(
    private val properties: ReplayCollectorProperties,
    private val indexParser: ApacheIndexParser,
) {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(properties.requestTimeout)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    private var lastRequestAt = 0L

    fun list(uri: URI): List<SourceEntry> = indexParser.parse(fetch(uri, null), uri)

    fun text(uri: URI): String = fetch(uri, properties.maxTextFileSize.toBytes())

    @Synchronized
    private fun fetch(uri: URI, maxBytes: Long?): String {
        val remainingDelay = properties.requestDelay.toNanos() - (System.nanoTime() - lastRequestAt)
        if (remainingDelay > 0) {
            val millis = remainingDelay / 1_000_000
            val nanos = (remainingDelay % 1_000_000).toInt()
            Thread.sleep(millis, nanos)
        }

        val request = HttpRequest.newBuilder(uri)
            .timeout(properties.requestTimeout)
            .header("Accept", "text/plain,text/html;q=0.9")
            .header("User-Agent", properties.userAgent)
            .GET()
            .build()
        LOGGER.debug { "GenTool data request: GET $uri" }
        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray())
        } finally {
            lastRequestAt = System.nanoTime()
        }
        if (response.statusCode() !in 200..299) {
            throw SourceRequestException("GET $uri returned HTTP ${response.statusCode()}")
        }
        if (maxBytes != null && response.body().size > maxBytes) {
            throw SourceRequestException("GET $uri exceeded the $maxBytes byte limit")
        }
        return String(response.body(), StandardCharsets.UTF_8)
    }

    private companion object {
        private val LOGGER = KotlinLogging.logger {}
    }
}

class SourceRequestException(message: String) : RuntimeException(message)