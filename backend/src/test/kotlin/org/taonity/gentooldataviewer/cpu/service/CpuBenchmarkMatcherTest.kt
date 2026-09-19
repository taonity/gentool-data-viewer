package org.taonity.gentooldataviewer.cpu.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource

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

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "AMD Athlon 200GE with Radeon Vega Graphics|AMD Athlon 200GE",
            "AMD Athlon 300U with Radeon Vega Mobile Gfx|AMD Athlon 300U",
            "AMD Athlon Gold 3150U with Radeon Graphics|AMD Athlon Gold 3150U",
            "AMD Athlon(tm) 5150 APU with Radeon(tm) R3|AMD Athlon 5150 APU",
            "AMD Ryzen 5 5600G with Radeon Graphics|AMD Ryzen 5 5600G",
            "AMD Ryzen 5 PRO 3400GE w/ Radeon Vega Graphics|AMD Ryzen 5 PRO 3400GE",
            "AMD Ryzen 7 7840HS with Radeon 780M Graphics|AMD Ryzen 7 7840HS",
            "AMD Ryzen 7 7840HS w/ Radeon 780M Graphics|AMD Ryzen 7 7840HS",
            "AMD Ryzen 7 H 255 w/ Radeon 780M Graphics|AMD Ryzen 7 H 255",
            "AMD Ryzen AI 9 HX 370 w/ Radeon 890M|AMD Ryzen AI 9 HX 370",
            "AMD Ryzen 3 1200 Quad-Core Processor|AMD Ryzen 3 1200",
            "AMD Ryzen 5 1600 Six-Core Processor|AMD Ryzen 5 1600",
            "AMD Ryzen 7 1700 Eight-Core Processor|AMD Ryzen 7 1700",
            "AMD Athlon(tm) X4 845 Quad Core Processor|AMD Athlon X4 845",
            "Intel(R) Core(TM)2 Duo CPU E8400 @ 3.00GHz|Intel Core2 Duo E8400 @ 3.00GHz",
            "Intel(R) Core(TM)2 CPU E8400 @ 3.00GHz|Intel Core2 Duo E8400 @ 3.00GHz",
            "Intel(R) Core(TM)2 Quad CPU Q9550 @ 2.83GHz|Intel Core2 Quad Q9550 @ 2.83GHz",
            "Intel(R) Core(TM)2 CPU 6320 @ 1.86GHz|Intel Core2 6320 @ 1.86GHz",
            "Intel(R) Core(TM) i7-2600 CPU @ 3.40GH|Intel Core i7-2600 @ 3.40GHz",
            "Intel(R) Core(TM)2 Quad CPU Q6600 @|Intel Core2 Quad Q6600 @ 2.40GHz",
            "Intel(R) Core(TM) i3 CPU M 350 @ 2.27GH|Intel Core i3-350M @ 2.27GHz",
            "Intel(R) Core(TM) i5 CPU M 430 @ 2.27GHz|Intel Core i5-430M @ 2.27GHz",
            "Intel(R) Core(TM) i7 CPU M 620 @ 2.67GHz|Intel Core i7-620M @ 2.67GHz",
            "Intel(R) Core(TM) i7 CPU Q 720 @ 1.60GHz|Intel Core i7-720QM @ 1.60GHz",
            "Intel(R) Core(TM) i7 CPU Q 740 @ 1.73GHz|Intel Core i7-740QM @ 1.73GHz",
            "Intel(R) Pentium(R) CPU P6100 @ 2.00GH|Intel Pentium P6100 @ 2.00GHz",
            "Intel(R) Pentium(R) Dual CPU E2160 @ 1.80GHz|Intel Pentium E2160 @ 1.80GHz",
            "Intel(R) Xeon(R) CPU E31220 @ 3.10GHz|Intel Xeon E3-1220 @ 3.10GHz",
            "Intel(R) Xeon(R) CPU E5-2670 0 @ 2.60GHz|Intel Xeon E5-2670 @ 2.60GHz",
        ],
    )
    fun `matches known CPU brand string formats`(rawName: String, catalogName: String) {
        val candidate = benchmark.copy(modelName = catalogName)

        val match = matcher.match(rawName, listOf(candidate))

        assertThat(match.status).isIn(CpuMatchStatus.EXACT, CpuMatchStatus.MODEL)
        assertThat(match.benchmark).isEqualTo(candidate)
    }

    @ParameterizedTest
    @CsvSource(
        delimiter = '|',
        value = [
            "AMD Ryzen 5 5600G with Radeon Graphics|AMD Ryzen 5 5600GE",
            "AMD Ryzen 5 PRO 5650U with Radeon Graphics|AMD Ryzen 5 5650U",
            "AMD Ryzen 7 7840H with Radeon 780M Graphics|AMD Ryzen 7 7840HS",
            "AMD Ryzen 7 7840HS w/ Radeon 780M Graphics|AMD Ryzen 7 7840HX",
            "AMD Ryzen 9 7945HX3D with Radeon Graphics|AMD Ryzen 9 7945HX",
            "AMD Ryzen 7 H 255 w/ Radeon 780M Graphics|AMD Ryzen 7 255",
            "AMD Ryzen AI 9 HX 375 w/ Radeon 890M|AMD Ryzen AI 9 HX 370",
            "Intel(R) Xeon(R) CPU E5-2670 0 @ 2.60GHz|Intel Xeon E5-2670 v2 @ 2.60GHz",
            "Intel Xeon E5-2670 v2 @ 2.60GHz|Intel Xeon E5-2670 v3 @ 2.30GHz",
            "Intel(R) Core(TM) i3 CPU M 350 @ 2.27GHz|Intel Core i3-350UM @ 1.20GHz",
        ],
    )
    fun `does not merge distinct CPU variants`(rawName: String, catalogName: String) {
        val match = matcher.match(rawName, listOf(benchmark.copy(modelName = catalogName)))

        assertThat(match.status).isEqualTo(CpuMatchStatus.UNMATCHED)
        assertThat(match.benchmark).isNull()
    }

    @ParameterizedTest
    @ValueSource(strings = [
        "Intel(R) Core i3 processor",
        "Intel(R) Core i7 processor",
        "Intel(R) Core(TM) i5 CPU",
        "Intel(R) Core(TM)2 Duo CPU",
        "Intel(R) Core(TM)2 Quad CPU",
        "Intel(R) Pentium(R) CPU E53",
        "Intel(R) Xeon(R) CPU",
        "X79 INTEL (INTEL Xeon E5/Corei7 DMI2 - C600/C200 Cipset V3.4E",
    ])
    fun `does not guess missing or truncated model numbers`(rawName: String) {
        val candidates = listOf(
            "Intel Core i3-350M @ 2.27GHz",
            "Intel Core i5-430M @ 2.27GHz",
            "Intel Core i7-2600 @ 3.40GHz",
            "Intel Core2 Duo E8400 @ 3.00GHz",
            "Intel Core2 Quad Q6600 @ 2.40GHz",
            "Intel Pentium E5300 @ 2.60GHz",
            "Intel Xeon E5-2670 @ 2.60GHz",
        ).mapIndexed { index, name -> benchmark.copy(sourceId = index.toString(), modelName = name) }

        assertThat(matcher.match(rawName, candidates).status).isEqualTo(CpuMatchStatus.UNMATCHED)
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
    fun `does not choose between catalog records that normalize to the same CPU`() {
        val candidates = listOf(
            benchmark.copy(sourceId = "1", modelName = "AMD Ryzen AI 5 330"),
            benchmark.copy(sourceId = "2", modelName = "AMD Ryzen AI 5 330 with Radeon 890M"),
        )

        val match = matcher.match("AMD Ryzen AI 5 330 w/ Radeon 820M", candidates)

        assertThat(match.status).isEqualTo(CpuMatchStatus.AMBIGUOUS)
        assertThat(match.benchmark).isNull()
    }

    @Test
    fun `prefers the exact clock when multiple clock variants exist`() {
        val candidates = listOf(
            benchmark.copy(sourceId = "1", modelName = "Intel Core2 Duo E8135 @ 2.40GHz"),
            benchmark.copy(sourceId = "2", modelName = "Intel Core2 Duo E8135 @ 2.66GHz"),
        )

        val exact = matcher.match("Intel(R) Core(TM)2 Duo CPU E8135 @ 2.66GHz", candidates)
        val truncated = matcher.match("Intel(R) Core(TM)2 Duo CPU E8135 @", candidates)

        assertThat(exact.status).isEqualTo(CpuMatchStatus.EXACT)
        assertThat(exact.benchmark).isEqualTo(candidates[1])
        assertThat(truncated.status).isEqualTo(CpuMatchStatus.AMBIGUOUS)
        assertThat(truncated.benchmark).isNull()
    }

    @Test
    fun `keeps absent and unknown CPUs unresolved`() {
        assertThat(matcher.match(null, listOf(benchmark)).status).isEqualTo(CpuMatchStatus.NO_CPU)
        assertThat(matcher.match("Unreleased Example CPU", listOf(benchmark)).status).isEqualTo(CpuMatchStatus.UNMATCHED)
    }
}