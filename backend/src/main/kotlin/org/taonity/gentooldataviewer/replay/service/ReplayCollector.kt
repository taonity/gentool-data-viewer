package org.taonity.gentooldataviewer.replay.service

import io.github.oshai.kotlinlogging.KotlinLogging
import org.taonity.gentooldataviewer.replay.client.GentoolSourceClient
import org.taonity.gentooldataviewer.replay.config.ReplayCollectorProperties
import org.taonity.gentooldataviewer.replay.parser.ReplayTextParser
import org.springframework.stereotype.Service
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CollectionProgress(
    var directoriesDiscovered: Long = 0,
    var directoriesScanned: Long = 0,
    var filesDiscovered: Long = 0,
    var filesImported: Long = 0,
    var filesSkipped: Long = 0,
    var failures: Long = 0,
)

@Service
class ReplayCollector(
    private val properties: ReplayCollectorProperties,
    private val sourceClient: GentoolSourceClient,
    private val textParser: ReplayTextParser,
    private val importService: ReplayImportService,
) {
    fun collect(
        startDate: LocalDate,
        endDate: LocalDate,
        userLimit: Int?,
        onProgress: (CollectionProgress) -> Unit,
    ): CollectionProgress {
        val progress = CollectionProgress()
        var date = startDate
        while (!date.isAfter(endDate) && !limitReached(progress, userLimit)) {
            collectDate(date, userLimit, progress, onProgress)
            date = date.plusDays(1)
        }
        onProgress(progress)
        return progress
    }

    private fun collectDate(
        date: LocalDate,
        userLimit: Int?,
        progress: CollectionProgress,
        onProgress: (CollectionProgress) -> Unit,
    ) {
        val dayUri = dayUri(date)
        val directories = try {
            sourceClient.list(dayUri).filter { it.directory }
        } catch (error: Exception) {
            progress.failures++
            LOGGER.warn(error) { "Could not list replay day $dayUri" }
            onProgress(progress)
            return
        }
        progress.directoriesDiscovered += directories.size
        onProgress(progress)

        val remaining = userLimit?.minus(progress.directoriesScanned.toInt())
        directories.take(remaining ?: directories.size).forEach { directory ->
            try {
                val textFiles = sourceClient.list(directory.uri)
                    .filter { !it.directory && it.name.endsWith(".txt", ignoreCase = true) }
                progress.filesDiscovered += textFiles.size
                textFiles.forEach { file -> collectFile(date, file.uri, progress) }
            } catch (error: Exception) {
                progress.failures++
                LOGGER.warn(error) { "Could not collect replay directory ${directory.uri}" }
            } finally {
                progress.directoriesScanned++
                if (progress.directoriesScanned % PROGRESS_UPDATE_INTERVAL == 0L) onProgress(progress)
            }
        }
    }

    private fun limitReached(progress: CollectionProgress, userLimit: Int?): Boolean =
        userLimit != null && progress.directoriesScanned >= userLimit

    private fun collectFile(date: LocalDate, uri: URI, progress: CollectionProgress) {
        val sourceUrl = uri.toASCIIString()
        if (importService.exists(sourceUrl)) {
            progress.filesSkipped++
            return
        }
        try {
            val parsed = textParser.parse(sourceClient.text(uri))
            if (importService.import(sourceUrl, date, parsed)) progress.filesImported++ else progress.filesSkipped++
        } catch (error: Exception) {
            progress.failures++
            LOGGER.warn(error) { "Could not import replay text $uri" }
        }
    }

    private fun dayUri(date: LocalDate): URI {
        val gameRoot = properties.baseUrl.resolve("${properties.game.trim('/')}/")
        return gameRoot.resolve("${date.format(MONTH_FOLDER)}/${date.format(DAY_FOLDER)}/")
    }

    private companion object {
        private val LOGGER = KotlinLogging.logger {}
        private val MONTH_FOLDER = DateTimeFormatter.ofPattern("yyyy_MM_MMMM", Locale.ENGLISH)
        private val DAY_FOLDER = DateTimeFormatter.ofPattern("dd_EEEE", Locale.ENGLISH)
        private const val PROGRESS_UPDATE_INTERVAL = 25L
    }
}