package org.example.fullstackstarter.replay.parser

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class ReplayTextParserTest {
    private val parser = ReplayTextParser()

    @Test
    fun `parses hardware match teams and replay metadata`() {
        val replay = parser.parse(SAMPLE)

        assertThat(replay.reporterName).isEqualTo("tao")
        assertThat(replay.reporterId).isEqualTo("8313DCDFD572")
        assertThat(replay.matchDate).isEqualTo(Instant.parse("2026-09-02T17:59:26Z"))
        assertThat(replay.matchLength).isEqualTo(Duration.ofMinutes(28).plusSeconds(25))
        assertThat(replay.windowsCompat).isEqualTo("6.2.9200 SP 0.0")
        assertThat(replay.repInfoInUse).isEqualTo("no")
        assertThat(replay.cpu).isEqualTo("13th Gen Intel(R) Core(TM) i7-13700K")
        assertThat(replay.fields["System"]).contains("PRO Z790-S WIFI", "NVIDIA GeForce RTX 4070 SUPER")
        assertThat(replay.fields["Install Type"]).isEqualTo("Modified Game Install: Not compatible with Regular Game")
        assertThat(replay.teams.flatMap { it.players }).extracting<String> { it.name }
            .containsExactly("geo", "tao", "vit", "odx")
        assertThat(replay.teams[0].players[0].army).isEqualTo("USA Laser")
        assertThat(replay.replayFileName).isEqualTo("17-59-26_2v2_geo_tao_vit_odx.rep")
        assertThat(replay.replaySizeBytes).isEqualTo(357198)
        assertThat(replay.associatedFiles).extracting<String> { it.name }
            .containsExactly("17-59-26_2v2_geo_tao_vit_odx.rep", "17-59-26_shot1.jpg")
        assertThat(replay.rawText).startsWith("GenTool Replay Information").endsWith("[76611 bytes]")
    }

    @Test
    fun `leaves cpu empty when GenTool reports only a graphics card`() {
        val replay = parser.parse(GPU_ONLY_SYSTEM_SAMPLE)

        assertThat(replay.system).isEqualTo("NVIDIA GeForce GT 630")
        assertThat(replay.cpu).isNull()
        assertThat(replay.fields["System"]).isEqualTo("NVIDIA GeForce GT 630")
    }

    private companion object {
        val SAMPLE = """
            GenTool Replay Information

            Windows (Compat): 6.2.9200 SP 0.0
            System:           American Megatrends International, LLC. 1.G1 1.G1
                              PRO Z790-S WIFI (MS-7D88) 1.0
                              Controller0-DIMMA2 F5-6000J3238F16G     16384
                              13th Gen Intel(R) Core(TM) i7-13700K
                              NVIDIA GeForce RTX 4070 SUPER

            GenTool Version:  8.9
            Player Name:      tao
            Player Id:        8313DCDFD572
            Match Date (UTC): 2026 Sep 02, 17:59:26
            Game Version:     Zero Hour 1.04 The First Decade (Mod)
            Install Type:     Modified Game Install: Not compatible with Regular Game
            RepInfo in use:   no

            Map Name:         maps/keep of the snow [rotr] [c]
            Start Cash:       10000
            Match Type:       2v2
            Match Length:     00:28:25
            Match Mode:       LAN

            Team 1
               1A593000 geo (USA Laser)
               1AB53000 tao (Random)
            Team 2
               1AA44000 vit (USA Superweapon)
               1A569000 odx (USA)


            Associated files: 17-59-26_2v2_geo_tao_vit_odx.rep [357198 bytes]
                              17-59-26_shot1.jpg [76611 bytes]
        """.trimIndent()

        val GPU_ONLY_SYSTEM_SAMPLE = """
            GenTool Replay Information

            Windows (Compat): 5.1.2600 SP 3.0
            System:           NVIDIA GeForce GT 630

            GenTool Version:  8.9
            Player Name:      'JWbotnhyxc
            Player Id:        JRUHAMENMOEE
            Match Date (UTC): 2026 Sep 02, 20:16:58
            Game Version:     Zero Hour 1.04 The First Decade
            Install Type:     Normal Game Install
            RepInfo in use:   no

            Map Name:         fortress avalanche
            Start Cash:       50000
            Match Type:       2v2
            Match Length:     00:12:22
            Match Mode:       Hamachi

            Team 1
               C0A80000 AK_47_DEKL (Random)
               C0A80000 'JWbotnhyxc (Random)
            Team 2
               C0A80000 MURAD (Random)
               C0A80000 DESKTOP-EKQT (Random)


            Associated files: 20-16-58_2v2_AK47DEKL_JWbotnhy_MURAD_DESKTOPE.rep [130755 bytes]
        """.trimIndent()
    }
}