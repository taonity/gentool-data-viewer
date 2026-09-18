package org.taonity.gentooldataviewer.replay.dto

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import java.time.Instant
import java.time.LocalDate

class MatchDatePeriodTest {
    @Test
    fun `includes the entire UTC end date`() {
        val period = MatchDatePeriod(LocalDate.parse("2026-09-08"), LocalDate.parse("2026-09-08"))
        assertThat(period.from).isEqualTo(Instant.parse("2026-09-08T00:00:00Z"))
        assertThat(period.until).isEqualTo(Instant.parse("2026-09-09T00:00:00Z"))
        assertThat(period.active).isTrue()
    }

    @Test
    fun `supports open bounds and all time`() {
        assertThat(MatchDatePeriod(null, null).active).isFalse()
        assertThat(MatchDatePeriod(null, LocalDate.parse("2026-09-08")).from).isNull()
        assertThat(MatchDatePeriod(LocalDate.parse("2026-09-08"), null).until).isNull()
    }

    @Test
    fun `rejects reversed dates`() {
        assertThatThrownBy { MatchDatePeriod(LocalDate.parse("2026-09-09"), LocalDate.parse("2026-09-08")) }
            .isInstanceOf(ResponseStatusException::class.java)
            .hasMessageContaining("400")
    }
}