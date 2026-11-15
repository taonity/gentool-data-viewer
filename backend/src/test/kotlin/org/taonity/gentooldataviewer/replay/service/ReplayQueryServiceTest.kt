package org.taonity.gentooldataviewer.replay.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.Mockito.RETURNS_DEFAULTS
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.taonity.gentooldataviewer.common.config.AppProperties
import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.console.config.ConsolePagingProperties
import org.taonity.gentooldataviewer.replay.entity.ReplayEntity
import org.taonity.gentooldataviewer.replay.repository.ReplayAssociatedFileRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayPlayerRepository
import org.taonity.gentooldataviewer.replay.repository.ReplayRepository
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

class ReplayQueryServiceTest {
    private val orders = mutableListOf<Sort>()
    private val repository = mock(ReplayRepository::class.java) { invocation ->
        when (invocation.method.name) {
            "browse", "search" -> {
                orders += invocation.arguments.filterIsInstance<Pageable>().single().sort
                Page.empty<ReplayEntity>()
            }
            else -> RETURNS_DEFAULTS.answer(invocation)
        }
    }
    private val settings = mock(AppSettings::class.java).apply {
        `when`(console()).thenReturn(ConsolePagingProperties(maxPageSize = 100, accessRequestsEnabled = false))
    }
    private val appProperties = mock(AppProperties::class.java).apply {
        `when`(replayMinMatchDate).thenReturn(LocalDate.parse("2026-09-04"))
    }
    private val service = ReplayQueryService(repository, mock(ReplayPlayerRepository::class.java),
        mock(ReplayAssociatedFileRepository::class.java), settings, appProperties, ObjectMapper())

    @Test
    fun `browse and search use index compatible default date order with stable pagination`() {
        service.list(null, null, 0, 50, null, null)
        service.list("Player", "reporter", 0, 50, "matchAt", "desc")
        assertThat(orders).hasSize(2).allSatisfy { sort ->
            assertThat(sort.toList()).containsExactly(Sort.Order.desc("matchAt"), Sort.Order.asc("id"))
            assertThat(sort.getOrderFor("matchAt")!!.nullHandling).isEqualTo(Sort.NullHandling.NATIVE)
        }
    }

    @Test
    fun `nullable replay columns still place missing values last in both directions`() {
        listOf("asc", "desc").forEach { service.list(null, null, 0, 50, "cpu", it) }
        assertThat(orders).allSatisfy { sort ->
            assertThat(sort.getOrderFor("cpu")!!.nullHandling).isEqualTo(Sort.NullHandling.NULLS_LAST)
            assertThat(sort.toList().last()).isEqualTo(Sort.Order.asc("id"))
        }
    }
}
