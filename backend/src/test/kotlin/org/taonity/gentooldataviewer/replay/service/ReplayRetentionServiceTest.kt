package org.taonity.gentooldataviewer.replay.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.taonity.gentooldataviewer.config.AppSettings
import org.taonity.gentooldataviewer.config.ConfigRegistry
import org.taonity.gentooldataviewer.config.ConfigValidationException
import org.taonity.gentooldataviewer.config.entity.ConfigOverrideEntity
import org.taonity.gentooldataviewer.config.repository.ConfigOverrideRepository
import org.taonity.gentooldataviewer.replay.entity.*
import org.taonity.gentooldataviewer.replay.parser.ParsedReplay
import org.taonity.gentooldataviewer.replay.repository.*
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.entity.ConsoleRole
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserInfo
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserPrincipal
import org.taonity.gentooldataviewer.user.repository.UserRepository
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Import(ReplayRetentionTestClock::class)
class ReplayRetentionServiceTest {
    @Autowired lateinit var service: ReplayRetentionService
    @Autowired lateinit var policy: ReplayRetentionPolicy
    @Autowired lateinit var importer: ReplayImportService
    @Autowired lateinit var replays: ReplayRepository
    @Autowired lateinit var hardware: PlayerHardwareRepository
    @Autowired lateinit var participants: ReplayPlayerRepository
    @Autowired lateinit var files: ReplayAssociatedFileRepository
    @Autowired lateinit var links: GentoolUserLinkRepository
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var overrides: ConfigOverrideRepository
    @Autowired lateinit var settings: AppSettings
    @Autowired lateinit var registry: ConfigRegistry
    @Autowired lateinit var mapper: ObjectMapper
    @Autowired lateinit var jdbc: JdbcTemplate
    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var retentionRepository: ReplayRetentionRepository
    @Autowired lateinit var transactionManager: PlatformTransactionManager

    @BeforeEach
    @AfterEach
    fun cleanUp() {
        overrides.deleteAll()
        settings.reload()
        links.deleteAll()
        hardware.deleteAll()
        participants.deleteAll()
        files.deleteAll()
        replays.deleteAll()
        users.deleteAll()
    }

    @Test
    fun `cleanup preserves boundary replays and account links and refreshes active summaries`() {
        val cutoff = policy.cutoff()
        val old = replay("old", "active", "Old name", cutoff.minusSeconds(1))
        replay("old-2", "active", "Old name", cutoff.minusSeconds(2))
        val boundary = replay("boundary", "active", "Boundary name", cutoff)
        val latest = replay("latest", "active", "Recent name", cutoff.plusSeconds(1))
        val inactive = replay("inactive", "inactive", "Inactive", cutoff.minusSeconds(1))
        hardware.saveAllAndFlush(listOf(
            PlayerHardwareEntity("active", "Recent name", mainName = "Old name", aliasesJson = "[\"Old name\"]",
                replayCount = 4, cpu = "Preserved CPU", cpuScore = 1000,
                observedAt = latest.matchAt, sourceReplayId = latest.id!!),
            PlayerHardwareEntity("inactive", "Inactive", observedAt = inactive.matchAt, sourceReplayId = inactive.id!!),
        ))
        participants.saveAndFlush(ReplayPlayerEntity(replayId = old.id!!, teamNumber = 1, slotNumber = 1, address = "1", name = "Old"))
        participants.saveAndFlush(ReplayPlayerEntity(replayId = boundary.id!!, teamNumber = 1, slotNumber = 1, address = "1", name = "Kept"))
        files.saveAndFlush(ReplayAssociatedFileEntity(replayId = old.id!!, fileName = "old.rep", sizeBytes = 1))
        files.saveAndFlush(ReplayAssociatedFileEntity(replayId = boundary.id!!, fileName = "kept.rep", sizeBytes = 1))
        users.saveAndFlush(UserEntity(googleId = "discord:retention", authProvider = "discord", displayName = "Retained user"))
        links.saveAndFlush(GentoolUserLinkEntity("discord:retention", "inactive", GentoolLinkStatus.APPROVED))

        assertThat(service.cleanup()).isEqualTo(ReplayRetentionResult(3, 1))
        assertThat(replays.findAll().map { it.id }).containsExactlyInAnyOrder(boundary.id, latest.id)
        assertThat(participants.findAll().map { it.name }).containsExactly("Kept")
        assertThat(files.findAll().map { it.fileName }).containsExactly("kept.rep")
        assertThat(hardware.existsById("inactive")).isFalse()
        val player = hardware.findById("active").orElseThrow()
        assertThat(player.mainName).isEqualTo("Recent name")
        assertThat(player.aliasesJson).isEqualTo("[\"Boundary name\"]")
        assertThat(player.replayCount).isEqualTo(2)
        assertThat(player.cpu).isEqualTo("Preserved CPU")
        assertThat(player.cpuScore).isEqualTo(1000)
        assertThat(player.sourceReplayId).isEqualTo(latest.id)
        assertThat(links.count()).isEqualTo(1)
        assertThat(users.existsById("discord:retention")).isTrue()
        assertThat(service.cleanup()).isEqualTo(ReplayRetentionResult(0, 0))
    }

    @Test
    fun `cleanup commits multiple batches and uses current configured retention`() {
        val matchAt = policy.cutoff().plusSeconds(1)
        val rows = (1..501).map { arrayOf<Any>("expired-$it", "https://example.invalid/expired-$it", "inactive", "Old", java.sql.Timestamp.from(matchAt)) }
        jdbc.batchUpdate(
            "INSERT INTO replay (id, source_url, source_date, reporter_id, reporter_name, match_at, collected_at, fields_json, raw_text, player_names) " +
                "VALUES (?, ?, CURRENT_DATE, ?, ?, ?, CURRENT_TIMESTAMP, '{}', '', 'Old')", rows,
        )
        setDays(1)
        assertThat(service.cleanup().replaysDeleted).isEqualTo(501)
        assertThat(replays.count()).isZero()
    }

    @Test
    fun `retention uses inclusive UTC calendar days and supports runtime overrides`() {
        assertThat(policy.window().startDate).isEqualTo(LocalDate.parse("2026-08-17"))
        assertThat(policy.window().endDate).isEqualTo(LocalDate.parse("2026-09-15"))
        assertThat(policy.accepts(LocalDate.parse("2026-08-17"), policy.cutoff())).isTrue()
        assertThat(policy.accepts(LocalDate.parse("2026-08-17"), policy.cutoff().minusNanos(1))).isFalse()
        assertThat(policy.accepts(LocalDate.parse("2026-08-16"), Instant.parse("2026-09-14T00:00:00Z"))).isFalse()
        assertThat(policy.accepts(LocalDate.parse("2026-09-15"), Instant.parse("2026-09-15T12:00:01Z"))).isFalse()
        setDays(1)
        assertThat(policy.window().startDate).isEqualTo(policy.window().endDate)
        val field = registry.field("app.retention.replays.retention-days")!!
        listOf("0", "3651").forEach { value ->
            assertThatThrownBy { registry.parse(field, mapper.readTree(value), settings.effective()) }
                .isInstanceOf(ConfigValidationException::class.java)
        }
        val cron = registry.field("app.retention.replays.cron")!!
        assertThatThrownBy { registry.parse(cron, mapper.readTree("\"invalid\""), settings.effective()) }
            .isInstanceOf(ConfigValidationException::class.java)
    }

    @Test
    fun `import rejects expired directory dates and expired match dates without creating hardware`() {
        val currentDate = policy.window().endDate
        val expired = policy.cutoff().minusSeconds(1)
        assertThat(importer.import("https://example.invalid/expired-match", currentDate, parsed(expired))).isFalse()
        assertThat(importer.import("https://example.invalid/expired-directory", policy.window().startDate.minusDays(1),
            parsed(policy.cutoff()))).isFalse()
        assertThat(replays.count()).isZero()
        assertThat(hardware.count()).isZero()
        assertThat(importer.import("https://example.invalid/boundary", policy.window().startDate, parsed(policy.cutoff()))).isTrue()
        assertThat(hardware.findAll().single().replayCount).isEqualTo(1)
    }

    @Test
    fun `collection API exposes retention and rejects expired scans with authentication and authorization`() {
        val principal = AuthenticatedUserPrincipal(emptyList(), mapOf("id" to "retention"),
            AuthenticatedUserInfo("discord", "retention", "Owner", null))
        users.saveAndFlush(UserEntity(googleId = "discord:retention", authProvider = "discord",
            displayName = "Owner", role = ConsoleRole.OWNER))
        mockMvc.perform(get("/console/replay-collection/window")).andExpect(status().isUnauthorized)
        mockMvc.perform(get("/console/replay-collection/window").with(oauth2Login().oauth2User(principal)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.retentionDays").value(30))
            .andExpect(jsonPath("$.startDate").value("2026-08-17"))
            .andExpect(jsonPath("$.endDate").value("2026-09-15"))
        mockMvc.perform(post("/console/replay-collection/jobs").with(oauth2Login().oauth2User(principal)).with(csrf())
            .contentType("application/json").content("""{"startDate":"2026-08-16","endDate":"2026-08-17"}"""))
            .andExpect(status().isBadRequest)
        val owner = users.findById("discord:retention").orElseThrow()
        owner.role = ConsoleRole.VIEWER
        users.saveAndFlush(owner)
        mockMvc.perform(post("/console/replay-collection/jobs").with(oauth2Login().oauth2User(principal)).with(csrf())
            .contentType("application/json").content("""{"startDate":"2026-09-15","endDate":"2026-09-15"}"""))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `imports and cleanup wait for the same database lock and preserve a new hardware source`() {
        val old = replay("old-source", "ABCDEF123456", "Old", policy.cutoff().minusSeconds(1))
        hardware.saveAndFlush(PlayerHardwareEntity("ABCDEF123456", "Old", observedAt = old.matchAt, sourceReplayId = old.id!!))
        val executor = Executors.newFixedThreadPool(3)
        val acquired = CountDownLatch(1)
        val release = CountDownLatch(1)
        try {
            val holder = executor.submit {
                TransactionTemplate(transactionManager).executeWithoutResult {
                    retentionRepository.lock()
                    acquired.countDown()
                    check(release.await(5, TimeUnit.SECONDS))
                }
            }
            check(acquired.await(5, TimeUnit.SECONDS))
            val imported = executor.submit<Boolean> {
                importer.import("https://example.invalid/new-source", policy.window().endDate,
                    parsed(Instant.parse("2026-09-15T10:00:00Z")))
            }
            val cleaned = executor.submit<ReplayRetentionResult> { service.cleanup() }
            assertThatThrownBy { imported.get(100, TimeUnit.MILLISECONDS) }.isInstanceOf(TimeoutException::class.java)
            assertThatThrownBy { cleaned.get(100, TimeUnit.MILLISECONDS) }.isInstanceOf(TimeoutException::class.java)
            release.countDown()
            holder.get(5, TimeUnit.SECONDS)
            assertThat(imported.get(5, TimeUnit.SECONDS)).isTrue()
            assertThat(cleaned.get(5, TimeUnit.SECONDS).replaysDeleted).isEqualTo(1)
            val current = hardware.findById("ABCDEF123456").orElseThrow()
            assertThat(replays.findById(current.sourceReplayId).orElseThrow().sourceUrl)
                .isEqualTo("https://example.invalid/new-source")
            assertThat(current.replayCount).isEqualTo(1)
        } finally {
            release.countDown()
            executor.shutdownNow()
            executor.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    private fun setDays(days: Long) {
        overrides.saveAndFlush(ConfigOverrideEntity(configKey = "app.retention.replays.retention-days", valueJson = days.toString(), updatedBy = "test"))
        settings.reload()
    }

    private fun replay(id: String, playerId: String, name: String, at: Instant): ReplayEntity = replays.saveAndFlush(
        ReplayEntity(sourceUrl = "https://example.invalid/$id", sourceDate = at.atOffset(ZoneOffset.UTC).toLocalDate(),
            reporterId = playerId, reporterName = name, playerNames = name, matchAt = at, fieldsJson = "{}", rawText = "fixture"),
    )

    private fun parsed(at: Instant) = ParsedReplay(
        rawText = "fixture", fields = emptyMap(), reporterName = "Player", reporterId = "ABCDEF123456", matchDate = at,
        windowsCompat = null, gentoolVersion = null, gameVersion = null, installType = null, repInfoInUse = null,
        mapName = null, startCash = null, matchType = null, matchLength = null, matchMode = null, system = null,
        cpu = null, teams = emptyList(), associatedFiles = emptyList(), replayFileName = null, replaySizeBytes = null,
    )
}
