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
        .replace(TRADEMARK_PATTERN, " ")
        .replace(GENERATION_PREFIX_PATTERN, " ")
        .replace(CORE_COUNT_PATTERN, " ")
        .replace(PROCESSOR_WORD_PATTERN, " ")
        .replace(NON_MODEL_CHARACTER_PATTERN, " ")
        .replace(WHITESPACE_PATTERN, " ")
        .trim()

    fun normalizeModel(value: String): String = normalize(value)
        .replace(CLOCK_PATTERN, " ")
        .replace(WHITESPACE_PATTERN, " ")
        .trim()

    private companion object {
        val TRADEMARK_PATTERN = Regex("\\((?:r|tm)\\)", RegexOption.IGNORE_CASE)
        val GENERATION_PREFIX_PATTERN = Regex("^\\s*\\d+(?:st|nd|rd|th)\\s+gen(?:eration)?\\s+", RegexOption.IGNORE_CASE)
        val CORE_COUNT_PATTERN = Regex("\\b\\d+[- ]core(?:s)?\\b", RegexOption.IGNORE_CASE)
        val PROCESSOR_WORD_PATTERN = Regex("\\b(?:cpu|processor)\\b", RegexOption.IGNORE_CASE)
        val CLOCK_PATTERN = Regex("\\s*@?\\s*\\d+(?:\\.\\d+)?\\s*(?:ghz|mhz)\\b.*$", RegexOption.IGNORE_CASE)
        val NON_MODEL_CHARACTER_PATTERN = Regex("[^a-z0-9@.]+")
        val WHITESPACE_PATTERN = Regex("\\s+")
    }
}