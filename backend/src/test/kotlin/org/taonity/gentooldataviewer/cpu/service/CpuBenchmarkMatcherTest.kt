package org.taonity.gentooldataviewer.cpu.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CpuBenchmarkMatcherTest {
    private val matcher = CpuBenchmarkMatcher()
    private val benchmark = CpuBenchmarkRecord("5060", "Intel Core i7-13700K", 4326, "https://example/5060")

    @Test
    fun `matches GenTool trademark and generation noise exactly`() {
        val match = matcher.match("13th Gen Intel(R) Core(TM) i7-13700K", listOf(benchmark))

        assertThat(match.status).isEqualTo(CpuMatchStatus.EXACT)
        assertThat(match.benchmark).isEqualTo(benchmark)
    }

    @Test
    fun `matches a unique model when clock formatting differs`() {
        val passMark = benchmark.copy(modelName = "Intel Core i7-2600 @ 3.40GHz")

        val match = matcher.match("Intel(R) Core(TM) i7-2600 CPU @ 3.41GHz", listOf(passMark))

        assertThat(match.status).isEqualTo(CpuMatchStatus.MODEL)
        assertThat(match.benchmark).isEqualTo(passMark)
    }

    @Test
    fun `does not guess between multiple model candidates`() {
        val candidates = listOf(
            benchmark.copy(sourceId = "1", modelName = "Intel Core i7-2600 @ 3.40GHz"),
            benchmark.copy(sourceId = "2", modelName = "Intel Core i7-2600 @ 3.50GHz"),
        )

        assertThat(matcher.match("Intel Core i7-2600", candidates).status).isEqualTo(CpuMatchStatus.AMBIGUOUS)
    }

    @Test
    fun `keeps absent and unknown CPUs unresolved`() {
        assertThat(matcher.match(null, listOf(benchmark)).status).isEqualTo(CpuMatchStatus.NO_CPU)
        assertThat(matcher.match("Unreleased Example CPU", listOf(benchmark)).status).isEqualTo(CpuMatchStatus.UNMATCHED)
    }
}