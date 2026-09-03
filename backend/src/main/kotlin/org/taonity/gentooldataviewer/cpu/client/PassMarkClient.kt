package org.taonity.gentooldataviewer.cpu.client

import org.taonity.gentooldataviewer.cpu.config.CpuBenchmarkProperties
import org.taonity.gentooldataviewer.cpu.service.CpuBenchmarkRecord
import org.springframework.stereotype.Component
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

@Component
class PassMarkClient(
    private val properties: CpuBenchmarkProperties,
    private val parser: PassMarkChartParser,
) {
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(properties.requestTimeout)
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()
    private var lastRequestAt = 0L

    fun fetchAll(): List<CpuBenchmarkRecord> {
        val first = fetchPage(properties.baseUrl)
        require(first.totalPages in 1..properties.maxPages) {
            "PassMark reported ${first.totalPages} pages; configured maximum is ${properties.maxPages}"
        }
        return buildList {
            addAll(first.benchmarks)
            for (page in 2..first.totalPages) {
                addAll(fetchPage(properties.baseUrl.resolve("page$page")).benchmarks)
            }
        }.distinctBy(CpuBenchmarkRecord::sourceId)
    }

    private fun fetchPage(uri: URI): PassMarkPage = parser.parse(fetch(uri), uri)

    @Synchronized
    private fun fetch(uri: URI): String {
        val remainingDelay = properties.requestDelay.toNanos() - (System.nanoTime() - lastRequestAt)
        if (remainingDelay > 0) {
            Thread.sleep(remainingDelay / 1_000_000, (remainingDelay % 1_000_000).toInt())
        }
        val request = HttpRequest.newBuilder(uri)
            .timeout(properties.requestTimeout)
            .header("Accept", "text/html")
            .header("User-Agent", properties.userAgent)
            .GET()
            .build()
        val response = try {
            httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        } finally {
            lastRequestAt = System.nanoTime()
        }
        require(response.statusCode() in 200..299) { "GET $uri returned HTTP ${response.statusCode()}" }
        return response.body()
    }
}