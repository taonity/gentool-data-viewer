package org.taonity.gentooldataviewer.cpu.client

import org.assertj.core.api.Assertions.assertThat
import org.taonity.gentooldataviewer.cpu.service.CpuBenchmarkRecord
import org.junit.jupiter.api.Test
import java.net.URI

class PassMarkChartParserTest {
    @Test
    fun `parses chart rows and page count`() {
        val page = PassMarkChartParser().parse(
            """
                <div class="page-nav-tab"><div class="active"><p>Page 1 (of 8)</p></div></div>
                <ul class="chartlist">
                  <li id="rk5060"><a href="/cpu.php?cpu=Intel+Core+i7-13700K&amp;id=5060">
                    <span class="prdname">Intel Core i7-13700K</span>
                    <span class="count">4,326</span>
                  </a></li>
                </ul>
            """.trimIndent(),
            URI.create("https://www.cpubenchmark.net/single-thread/"),
        )

        assertThat(page.totalPages).isEqualTo(8)
        assertThat(page.benchmarks).containsExactly(
            CpuBenchmarkRecord(
                "5060",
                "Intel Core i7-13700K",
                4326,
                "https://www.cpubenchmark.net/cpu.php?cpu=Intel+Core+i7-13700K&id=5060",
            ),
        )
    }
}