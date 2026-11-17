package org.taonity.gentooldataviewer.console

import org.taonity.gentooldataviewer.console.entity.AuditAction
import org.taonity.gentooldataviewer.console.entity.AuditLogEntity
import org.taonity.gentooldataviewer.console.repository.AuditLogRepository
import org.taonity.gentooldataviewer.other.ControllerTestsBaseClass
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserPrincipal
import org.taonity.gentooldataviewer.security.principal.AuthenticatedUserInfo
import org.taonity.gentooldataviewer.user.entity.UserEntity
import org.taonity.gentooldataviewer.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@ActiveProfiles("h2", inheritProfiles = false)
class ConsoleDataControllerTest : ControllerTestsBaseClass() {

    @Autowired
    private lateinit var auditLogRepository: AuditLogRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        auditLogRepository.deleteAll()
        userRepository.save(
            UserEntity(
                googleId = TEST_USER_ID,
                authProvider = "discord",
                displayName = "Owner",
            ).grantOwner(),
        )
        auditLogRepository.saveAll(
            listOf(
                auditLog(AuditAction.RESET_CONFIG, "z-record", Instant.parse("2026-01-01T00:00:00Z")),
                auditLog(AuditAction.APPROVE_ACCESS, "a-record", Instant.parse("2026-01-03T00:00:00Z")),
                auditLog(AuditAction.CHANGE_ROLE, "m-record", Instant.parse("2026-01-02T00:00:00Z")),
            ),
        )
    }

    @Test
    fun `sorts unfiltered and searched audit logs by requested columns`() {
        mockMvc.perform(
            get("/console/audit-logs")
                .with(oauth2Login().oauth2User(principal()))
                .param("sort", "action")
                .param("direction", "asc"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].action").value("APPROVE_ACCESS"))
            .andExpect(jsonPath("$.content[1].action").value("CHANGE_ROLE"))
            .andExpect(jsonPath("$.content[2].action").value("RESET_CONFIG"))

        mockMvc.perform(
            get("/console/audit-logs")
                .with(oauth2Login().oauth2User(principal()))
                .param("q", "record")
                .param("sort", "targetId")
                .param("direction", "desc"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].targetId").value("z-record"))
            .andExpect(jsonPath("$.content[1].targetId").value("m-record"))
            .andExpect(jsonPath("$.content[2].targetId").value("a-record"))
    }

    @Test
    fun `exact match is case insensitive and scoped to the selected field`() {
        mockMvc.perform(get("/console/audit-logs").with(oauth2Login().oauth2User(principal()))
            .param("q", "a-record").param("field", "targetId").param("exact", "true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].action").value("APPROVE_ACCESS"))

        mockMvc.perform(get("/console/audit-logs").with(oauth2Login().oauth2User(principal()))
            .param("q", "RECORD").param("field", "targetId").param("exact", "true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))

        mockMvc.perform(get("/console/audit-logs").with(oauth2Login().oauth2User(principal()))
            .param("q", "change_role").param("field", "action").param("exact", "true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))

        mockMvc.perform(get("/console/audit-logs").with(oauth2Login().oauth2User(principal()))
            .param("q", "audit@example.com").param("field", "targetId").param("exact", "true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    @Test
    fun `date periods use inclusive UTC calendar days and combine with search and pagination`() {
        auditLogRepository.save(auditLog(AuditAction.RESET_CONFIG, "end-of-day", Instant.parse("2026-01-02T23:59:59.999Z")))
        mockMvc.perform(get("/console/audit-logs").with(oauth2Login().oauth2User(principal()))
            .param("startDate", "2026-01-02").param("endDate", "2026-01-02")
            .param("sort", "occurredAt").param("direction", "asc").param("size", "1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].targetId").value("m-record"))
            .andExpect(jsonPath("$.totalPages").value(2))

        mockMvc.perform(get("/console/audit-logs").with(oauth2Login().oauth2User(principal()))
            .param("startDate", "2026-01-02").param("endDate", "2026-01-02")
            .param("q", "END-OF-DAY").param("field", "targetId").param("exact", "true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].targetId").value("end-of-day"))
    }

    @Test
    fun `date bounds can be used independently`() {
        mockMvc.perform(get("/console/audit-logs").with(oauth2Login().oauth2User(principal()))
            .param("startDate", "2026-01-02"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
        mockMvc.perform(get("/console/audit-logs").with(oauth2Login().oauth2User(principal()))
            .param("endDate", "2026-01-02"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `invalid date periods return a client error`() {
        mockMvc.perform(get("/console/audit-logs").with(oauth2Login().oauth2User(principal()))
            .param("startDate", "2026-01-03").param("endDate", "2026-01-01"))
            .andExpect(status().isBadRequest)
        mockMvc.perform(get("/console/audit-logs").with(oauth2Login().oauth2User(principal()))
            .param("startDate", "2026-02-30"))
            .andExpect(status().isBadRequest)
        mockMvc.perform(get("/console/audit-logs").with(oauth2Login().oauth2User(principal()))
            .param("endDate", "invalid"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `date and exact search remain restricted to admins`() {
        mockMvc.perform(get("/console/audit-logs").param("exact", "true").param("startDate", "2026-01-01"))
            .andExpect(status().isUnauthorized)
        userRepository.save(UserEntity(googleId = TEST_USER_ID, authProvider = "discord", displayName = "Viewer"))
        mockMvc.perform(get("/console/audit-logs").with(oauth2Login().oauth2User(principal()))
            .param("exact", "true").param("startDate", "2026-01-01"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `reports available audit dates in UTC and supports an empty log`() {
        mockMvc.perform(get("/console/audit-logs/date-range").with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.startDate").value("2026-01-01"))
            .andExpect(jsonPath("$.endDate").value("2026-01-03"))
        auditLogRepository.deleteAll()
        mockMvc.perform(get("/console/audit-logs/date-range").with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.startDate").isEmpty)
            .andExpect(jsonPath("$.endDate").isEmpty)
    }

    @Test
    fun `available audit dates remain restricted to admins`() {
        mockMvc.perform(get("/console/audit-logs/date-range"))
            .andExpect(status().isUnauthorized)
        userRepository.save(UserEntity(googleId = TEST_USER_ID, authProvider = "discord", displayName = "Viewer"))
        mockMvc.perform(get("/console/audit-logs/date-range").with(oauth2Login().oauth2User(principal())))
            .andExpect(status().isForbidden)
    }

    private fun auditLog(action: AuditAction, targetId: String, occurredAt: Instant) = AuditLogEntity(
        action = action,
        targetType = "CONFIG",
        targetId = targetId,
        actorUserId = "discord:audit-test-user",
        occurredAt = occurredAt,
    )

    private fun principal() = AuthenticatedUserPrincipal(
        authorities = emptyList(),
        attributes = mapOf("id" to "audit-test-owner"),
        userInfo = AuthenticatedUserInfo("discord", "audit-test-owner", "Owner", null),
    )

    companion object {
        private const val TEST_USER_ID = "discord:audit-test-owner"
    }
}