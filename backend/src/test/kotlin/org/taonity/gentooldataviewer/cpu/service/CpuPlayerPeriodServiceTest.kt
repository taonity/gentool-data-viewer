package org.taonity.gentooldataviewer.cpu.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import org.taonity.gentooldataviewer.cpu.entity.CpuBenchmarkEntity
import org.taonity.gentooldataviewer.cpu.repository.CpuBenchmarkRepository
import org.taonity.gentooldataviewer.replay.dto.MatchDatePeriod
import org.taonity.gentooldataviewer.replay.entity.*
import org.taonity.gentooldataviewer.replay.repository.*
import org.taonity.gentooldataviewer.replay.service.ReplayQueryService
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.repository.UserRepository
import java.time.Instant
import java.time.LocalDate

@SpringBootTest
@ActiveProfiles("h2")
@Transactional
class CpuPlayerPeriodServiceTest {
    @Autowired lateinit var service: CpuPlayerPeriodService
    @Autowired lateinit var replayQuery: ReplayQueryService
    @Autowired lateinit var replays: ReplayRepository
    @Autowired lateinit var participants: ReplayPlayerRepository
    @Autowired lateinit var files: ReplayAssociatedFileRepository
    @Autowired lateinit var hardware: PlayerHardwareRepository
    @Autowired lateinit var links: GentoolUserLinkRepository
    @Autowired lateinit var users: UserRepository
    @Autowired lateinit var benchmarks: CpuBenchmarkRepository
    @Autowired lateinit var matcher: CpuBenchmarkMatcher
    @Autowired lateinit var periodRepository: org.taonity.gentooldataviewer.cpu.repository.CpuPlayerPeriodRepository

    private val day = LocalDate.parse("2026-09-08")

    @BeforeEach
    fun prepare() {
        links.deleteAll()
        participants.deleteAll()
        files.deleteAll()
        hardware.deleteAll()
        replays.deleteAll()
        benchmarks.deleteAll()
        users.deleteAll()
        val cpu = "Intel Core i7-13700K"
        benchmarks.saveAndFlush(CpuBenchmarkEntity("old-cpu", cpu, matcher.normalize(cpu), matcher.normalizeModel(cpu), 4326,
            "https://example.invalid/cpu", Instant.parse("2026-09-01T00:00:00Z")))
        report("before-cutoff", "A", "Outside", "2026-09-03T23:59:59Z")
        report("at-cutoff", "A", "At cutoff", "2026-09-04T00:00:00Z")
        report("start", "A", "Alpha", "2026-09-08T00:00:00Z", cpu)
        report("middle", "A", "Alpha", "2026-09-08T12:00:00Z", cpu)
        report("end", "A", "Alias", "2026-09-08T23:59:59.999Z", cpu)
        report("other", "B", "Bravo", "2026-09-08T12:00:00Z")
        val future = report("after", "A", "Future", "2026-09-09T00:00:00Z", "New CPU")
        hardware.saveAndFlush(PlayerHardwareEntity("A", "Future", mainName = "All time", replayCount = 5,
            cpu = "New CPU", observedAt = future.matchAt, sourceReplayId = requireNotNull(future.id)))
        users.saveAndFlush(UserEntity(googleId = "discord:period", displayName = "Period user", authProvider = "discord"))
        links.saveAndFlush(GentoolUserLinkEntity("discord:period", "A", GentoolLinkStatus.APPROVED))
    }

    private fun report(id: String, player: String, name: String, at: String, cpu: String? = null): ReplayEntity {
        val saved = replays.saveAndFlush(ReplayEntity(sourceUrl = "https://example.invalid/$id", sourceDate = day,
            reporterId = player, reporterName = name, playerNames = name, matchAt = Instant.parse(at), cpu = cpu,
            fieldsJson = "{}", rawText = "fixture"))
        participants.saveAndFlush(ReplayPlayerEntity(replayId = requireNotNull(saved.id), teamNumber = 1, slotNumber = 1,
            address = "1A", name = name))
        return saved
    }

    @Test
    fun `period aggregates names counts latest CPU lineup and rank without modifying lifetime data`() {
        val period = MatchDatePeriod(day, day)
        val expectedLatest = replays.findAll().single { it.sourceUrl.endsWith("/end") }
        val projected = periodRepository.aggregate(period).filter { it.playerId == "A" }
        assertThat(projected.map { it.latestReplayId }).describedAs("Period latest replay IDs: %s", projected)
            .containsOnly(requireNotNull(expectedLatest.id))
        val page = service.list(period, null, null, 0, 1, "replayCount", "desc", false, null, false)
        assertThat(page.totalElements).isEqualTo(2)
        assertThat(page.hasMore).isTrue()
        val player = page.content.single()
        assertThat(player.mainName).isEqualTo("Alpha")
        assertThat(player.aliases).containsExactly("Alias")
        assertThat(player.latestName).isEqualTo("Alias")
        assertThat(player.replayCount).isEqualTo(3)
        assertThat(player.reportedCpu).isEqualTo("Intel Core i7-13700K")
        assertThat(player.singleThreadScore).isEqualTo(4326)
        assertThat(player.latestMatch?.teams).containsExactly(listOf("Alias"))
        assertThat(service.linked(period, "discord:period", "replayCount", "asc", false).single().rank).isEqualTo(2)
        assertThat(service.linked(period, "discord:period", "replayCount", "asc", true).single().rank).isEqualTo(1)
        assertThat(service.list(period, "3", "replayCount", 0, 10, "score", "desc", true, null, true).content).hasSize(1)
        assertThat(service.list(period, "Future", "latestName", 0, 10, null, null, false, null, true).content).isEmpty()
        assertThat(service.list(period, null, null, 1, 1, "replayCount", "desc", false, null, false).content.single().playerId).isEqualTo("B")
        assertThat(service.summary(period).totalPlayers).isEqualTo(2)
        assertThat(service.summary(period).ratedPlayers).isEqualTo(1)
        assertThat(service.list(period, null, null, 0, 10, "score", "asc", false, null, false).content.map { it.playerId })
            .containsExactly("A", "B")
        assertThat(service.list(period, null, null, 0, 10, "score", "desc", false, null, false).content.map { it.playerId })
            .containsExactly("A", "B")
        val emptyPeriod = MatchDatePeriod(day.plusDays(2), null)
        assertThat(service.linked(emptyPeriod, "discord:period", "replayCount", "desc", false)).isEmpty()
        assertThat(service.summary(emptyPeriod).totalPlayers).isZero()
        assertThat(hardware.findById("A").orElseThrow().mainName).isEqualTo("All time")
        assertThat(hardware.findById("A").orElseThrow().cpu).isEqualTo("New CPU")
    }

    @Test
    fun `available date range uses all replay match dates in UTC`() {
        val range = replayQuery.dateRange()
        assertThat(range.startDate).isEqualTo(LocalDate.parse("2026-09-04"))
        assertThat(range.endDate).isEqualTo(day.plusDays(1))
    }

    @Test
    fun `available date range is empty when no replays exist`() {
        links.deleteAll()
        participants.deleteAll()
        files.deleteAll()
        hardware.deleteAll()
        replays.deleteAll()
        val range = replayQuery.dateRange()
        assertThat(range.startDate).isNull()
        assertThat(range.endDate).isNull()
    }

    @Test
    fun `replay range includes both day boundaries and intersects other filters`() {
        val page = replayQuery.list(null, null, 0, 10, "matchAt", "asc", startDate = day, endDate = day)
        assertThat(page.totalElements).isEqualTo(4)
        assertThat(page.content.first().matchAt).isEqualTo(Instant.parse("2026-09-08T00:00:00Z"))
        assertThat(page.content.last().matchAt).isEqualTo(Instant.parse("2026-09-08T23:59:59.999Z"))
        assertThat(replayQuery.list("Alias", "players", 0, 10, null, null, listOf("A"), exact = true,
            startDate = day, endDate = day).totalElements).isEqualTo(1)
        assertThat(replayQuery.list(null, null, 0, 10, null, null, startDate = day.plusDays(2)).content).isEmpty()
        assertThat(replayQuery.list(null, null, 0, 10, null, null, endDate = day.minusDays(1)).totalElements).isEqualTo(1)
        assertThat(replayQuery.list(null, null, 0, 10, null, null).totalElements).isEqualTo(6)
    }
}