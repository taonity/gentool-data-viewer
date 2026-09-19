package org.taonity.gentooldataviewer.cpu.service

import org.springframework.stereotype.Component
import java.util.Locale

enum class CpuMatchStatus {
    EXACT,
    MODEL,
    AMBIGUOUS,
    UNMATCHED,
    NO_CPU,
}

data class CpuBenchmarkRecord(
    val sourceId: String,
    val modelName: String,
    val singleThreadScore: Int,
    val sourceUrl: String,
)

data class CpuMatch(
    val status: CpuMatchStatus,
    val benchmark: CpuBenchmarkRecord? = null,
)

@Component
class CpuBenchmarkMatcher {
    fun match(cpu: String?, benchmarks: Collection<CpuBenchmarkRecord>): CpuMatch {
        if (cpu.isNullOrBlank()) return CpuMatch(CpuMatchStatus.NO_CPU)

        val exactKey = normalize(cpu)
        val exact = benchmarks.filter { normalize(it.modelName) == exactKey }
        if (exact.size == 1) return CpuMatch(CpuMatchStatus.EXACT, exact.single())
        if (exact.size > 1) return CpuMatch(CpuMatchStatus.AMBIGUOUS)

        val modelKey = normalizeModel(cpu)
        val modelMatches = benchmarks.filter { normalizeModel(it.modelName) == modelKey }
        return when (modelMatches.size) {
            0 -> CpuMatch(CpuMatchStatus.UNMATCHED)
            1 -> CpuMatch(CpuMatchStatus.MODEL, modelMatches.single())
            else -> CpuMatch(CpuMatchStatus.AMBIGUOUS)
        }
    }

    fun normalize(value: String): String = value
        .lowercase(Locale.ENGLISH)
        .replace(TRADEMARK_PATTERN, "")
        .replace(GENERATION_PREFIX_PATTERN, " ")
        .replace(AMD_GRAPHICS_SUFFIX_PATTERN, "$1")
        .replace(CORE_COUNT_PATTERN, " ")
        .replace(PROCESSOR_WORD_PATTERN, " ")
        .replace(NON_MODEL_CHARACTER_PATTERN, " ")
        .replace(WHITESPACE_PATTERN, " ")
        .trim()
        .replace(INTEL_CORE2_PATTERN, "intel core2")
        .replace(INTEL_CORE2_DUO_PATTERN, "$1 $2")
        .replace(INTEL_MOBILE_PATTERN, "$1 $2m")
        .replace(INTEL_MOBILE_QUAD_PATTERN, "$1 $2qm")
        .replace(INTEL_PENTIUM_DUAL_PATTERN, "$1 $2")
        .replace(XEON_COMPACT_MODEL_PATTERN, "$1 $2 $3")
        .replace(XEON_ZERO_SUFFIX_PATTERN, "$1")

    fun normalizeModel(value: String): String = normalize(value)
        .replace(CLOCK_PATTERN, " ")
        .replace(TRAILING_CLOCK_MARKER_PATTERN, " ")
        .replace(WHITESPACE_PATTERN, " ")
        .trim()

    private companion object {
        val TRADEMARK_PATTERN = Regex("\\((?:r|tm)\\)", RegexOption.IGNORE_CASE)
        val GENERATION_PREFIX_PATTERN = Regex("^\\s*\\d+(?:st|nd|rd|th)\\s+gen(?:eration)?\\s+", RegexOption.IGNORE_CASE)
        val AMD_GRAPHICS_SUFFIX_PATTERN = Regex("^(\\s*amd\\s+(?:ryzen|athlon)\\b.*?)\\s+(?:with|w/)\\s+radeon\\b.*$")
        val CORE_COUNT_PATTERN = Regex("\\b(?:\\d+|dual|quad|six|eight)[- ]core(?:s)?\\b", RegexOption.IGNORE_CASE)
        val PROCESSOR_WORD_PATTERN = Regex("\\b(?:cpu|processor)\\b", RegexOption.IGNORE_CASE)
        val INTEL_CORE2_PATTERN = Regex("^intel core 2\\b")
        val INTEL_CORE2_DUO_PATTERN = Regex("^(intel core2) duo ([ept]\\d{4})(?=\\s|$)")
        val INTEL_MOBILE_PATTERN = Regex("^(intel core i[357]) m (\\d{3})(?=\\s|$)")
        val INTEL_MOBILE_QUAD_PATTERN = Regex("^(intel core i7) q (\\d{3})(?=\\s|$)")
        val INTEL_PENTIUM_DUAL_PATTERN = Regex("^(intel pentium) dual (e\\d{4})(?=\\s|$)")
        val XEON_COMPACT_MODEL_PATTERN = Regex("^(intel xeon) (e[357])(\\d{4}[a-z]*)(?=\\s|$)")
        val XEON_ZERO_SUFFIX_PATTERN = Regex("^(intel xeon e[357] \\d{4}[a-z]*) 0(?=\\s*@|$)")
        val CLOCK_PATTERN = Regex("\\s*@?\\s*\\d+(?:\\.\\d+)?\\s*(?:ghz|mhz|gh)\\b.*$", RegexOption.IGNORE_CASE)
        val TRAILING_CLOCK_MARKER_PATTERN = Regex("\\s*@\\s*$")
        val NON_MODEL_CHARACTER_PATTERN = Regex("[^a-z0-9@.]+")
        val WHITESPACE_PATTERN = Regex("\\s+")
    }
}