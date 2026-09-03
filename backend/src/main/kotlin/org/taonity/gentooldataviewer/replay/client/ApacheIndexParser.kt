package org.taonity.gentooldataviewer.replay.client

import org.jsoup.Jsoup
import org.springframework.stereotype.Component
import java.net.URI

data class SourceEntry(
    val name: String,
    val uri: URI,
    val directory: Boolean,
)

@Component
class ApacheIndexParser {
    fun parse(html: String, indexUri: URI): List<SourceEntry> =
        Jsoup.parse(html, indexUri.toString())
            .select("a[href]")
            .mapNotNull { anchor ->
                val href = anchor.attr("href")
                if (href == "../" || href.startsWith('?') || href.startsWith('#')) return@mapNotNull null

                val uri = runCatching { URI.create(anchor.absUrl("href")) }.getOrNull() ?: return@mapNotNull null
                if (uri.host != indexUri.host || !uri.path.startsWith(indexUri.path)) return@mapNotNull null
                SourceEntry(
                    name = anchor.text().removeSuffix("/"),
                    uri = uri,
                    directory = href.substringBefore('?').endsWith('/'),
                )
            }
            .distinctBy(SourceEntry::uri)
}