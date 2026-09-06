package org.taonity.gentooldataviewer.replay.service

import org.assertj.core.api.Assertions.assertThat
import org.taonity.gentooldataviewer.replay.client.GentoolSourceClient
import org.taonity.gentooldataviewer.replay.client.SourceEntry
import org.taonity.gentooldataviewer.replay.config.ReplayCollectorProperties
import org.taonity.gentooldataviewer.replay.parser.ReplayTextParser
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.util.unit.DataSize
import java.net.URI
import java.time.Duration
import java.time.LocalDate

class ReplayCollectorTest {
    private val sourceClient = mock(GentoolSourceClient::class.java)
    private val textParser = mock(ReplayTextParser::class.java)
    private val importService = mock(ReplayImportService::class.java)
    private val collector = ReplayCollector(properties(), sourceClient, textParser, importService)

    @Test
    fun `stops after optional user limit`() {
        val day = URI.create("https://gentool.net/data/zh/2026_09_September/02_Wednesday/")
        val directories = (1..3).map { number ->
            SourceEntry("user$number", day.resolve("user$number/"), true)
        }
        `when`(sourceClient.list(day)).thenReturn(directories)
        directories.forEach { `when`(sourceClient.list(it.uri)).thenReturn(emptyList()) }

        val progress = collector.collect(LocalDate.parse("2026-09-02"), LocalDate.parse("2026-09-02"), 2) {}

        assertThat(progress.directoriesDiscovered).isEqualTo(3)
        assertThat(progress.directoriesScanned).isEqualTo(2)
        verify(sourceClient).list(directories[0].uri)
        verify(sourceClient).list(directories[1].uri)
        verify(sourceClient, never()).list(directories[2].uri)
    }

    @Test
    fun `targeted rescan scans only matching player directory`() {
        val day = URI.create("https://gentool.net/data/zh/2026_09_September/02_Wednesday/")
        val target = SourceEntry("Player_ABCDEF123456", day.resolve("Player_ABCDEF123456/"), true)
        val other = SourceEntry("Other_123456ABCDEF", day.resolve("Other_123456ABCDEF/"), true)
        `when`(sourceClient.list(day)).thenReturn(listOf(other, target))
        `when`(sourceClient.list(target.uri)).thenReturn(emptyList())

        val progress = collector.collect(
            LocalDate.parse("2026-09-02"),
            LocalDate.parse("2026-09-02"),
            null,
            "abcdef123456",
        ) {}

        assertThat(progress.directoriesDiscovered).isEqualTo(1)
        assertThat(progress.directoriesScanned).isEqualTo(1)
        verify(sourceClient).list(target.uri)
        verify(sourceClient, never()).list(other.uri)
    }

    private fun properties() = ReplayCollectorProperties(
        baseUrl = URI.create("https://gentool.net/data/"),
        game = "zh",
        requestDelay = Duration.ZERO,
        requestTimeout = Duration.ofSeconds(1),
        maxTextFileSize = DataSize.ofKilobytes(64),
        maxDaysPerJob = 31,
        userAgent = "test",
        scheduleEnabled = false,
        scheduleCron = "0 15 2 * * *",
        scheduleZone = "UTC",
    )
}