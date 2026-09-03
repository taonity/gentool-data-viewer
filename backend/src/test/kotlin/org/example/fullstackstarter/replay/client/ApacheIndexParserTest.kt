package org.example.fullstackstarter.replay.client

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.URI

class ApacheIndexParserTest {
    private val parser = ApacheIndexParser()

    @Test
    fun `keeps child links and ignores sorting parent and external links`() {
        val entries = parser.parse(
            """
                <a href="?C=N;O=D">Name</a>
                <a href="../">Parent Directory</a>
                <a href="tao_8313DCDFD572/">tao_8313DCDFD572/</a>
                <a href="%5BCaP%5DBlaCk_A78FD864843A/">[CaP]BlaCk_A78FD864843A/</a>
                <a href="17-59-26_2v2_geo_tao_vit_odx.txt">replay.txt</a>
                <a href="https://example.com/file.txt">external</a>
            """.trimIndent(),
            URI.create("https://gentool.net/data/zh/2026_09_September/02_Wednesday/"),
        )

        assertThat(entries).extracting<String> { it.name }
            .containsExactly("tao_8313DCDFD572", "[CaP]BlaCk_A78FD864843A", "replay.txt")
        assertThat(entries.take(2)).allMatch { it.directory }
        assertThat(entries.last().directory).isFalse()
        assertThat(entries[1].uri.rawPath).contains("%5BCaP%5D")
    }
}