package org.example.fullstackstarter.cpu.client

import org.example.fullstackstarter.cpu.service.CpuBenchmarkRecord
import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import java.net.URI

data class PassMarkPage(
    val benchmarks: List<CpuBenchmarkRecord>,
    val totalPages: Int,
)

@Component
class PassMarkChartParser {
    fun parse(html: String, pageUri: URI): PassMarkPage {
        val document = Jsoup.parse(html, pageUri.toString())
        val benchmarks = document.select("ul.chartlist > li").mapNotNull { row ->
            val sourceId = row.id().removePrefix("rk").takeIf(String::isNotBlank) ?: return@mapNotNull null
            val modelName = row.selectFirst(".prdname")?.text()?.trim().orEmpty()
            val score = row.selectFirst(".count")?.text()?.replace(",", "")?.toIntOrNull()
            val sourceUrl = row.selectFirst("a[href]")?.absUrl("href").orEmpty()
            if (modelName.isBlank() || score == null || sourceUrl.isBlank()) return@mapNotNull null
            CpuBenchmarkRecord(sourceId, modelName, score, sourceUrl)
        }
        val totalPages = PAGE_COUNT_PATTERN.find(document.select(".page-nav-tab").text())
            ?.groupValues
            ?.get(1)
            ?.toIntOrNull()
            ?: 1
        return PassMarkPage(benchmarks, totalPages)
    }

    private companion object {
        val PAGE_COUNT_PATTERN = Regex("\\(of (\\d+)\\)", RegexOption.IGNORE_CASE)
    }
}