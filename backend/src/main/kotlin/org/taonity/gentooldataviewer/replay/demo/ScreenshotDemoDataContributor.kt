package org.taonity.gentooldataviewer.replay.demo

import org.springframework.context.annotation.Profile
import org.springframework.core.annotation.Order
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import org.taonity.gentooldataviewer.common.demo.DemoDataContributor
import java.sql.Timestamp
import java.time.Clock

@Component
@Profile("demo")
@Order(500)
class ScreenshotDemoDataContributor(private val jdbc: JdbcTemplate, private val clock: Clock) : DemoDataContributor {
    override val feature: String = "screenshot-timestamps"

    @Transactional
    override fun seed(): Int {
        val time = Timestamp.from(clock.instant().minusSeconds(600))
        var updated = jdbc.update("UPDATE replay SET collected_at = ? WHERE collected_at <> ?", time, time)
        updated += jdbc.update(
            "UPDATE player_hardware SET gentool_updated_at = ?, cpu_score_updated_at = CASE WHEN cpu_score IS NULL THEN NULL ELSE ? END WHERE gentool_updated_at <> ?",
            time, time, time,
        )
        updated += jdbc.update(
            "INSERT INTO app_user (user_id, display_name, auth_provider, role, access_status) SELECT 'discord:100000000000000000', 'Test Owner', 'discord', 'OWNER', 'APPROVED' WHERE NOT EXISTS (SELECT 1 FROM app_user WHERE user_id = 'discord:100000000000000000')",
        )
        updated += jdbc.update(
            "INSERT INTO gentool_user_link (user_id, player_id, status, requested_at, decided_at, decided_by_user_id) SELECT 'discord:100000000000000000', 'D3A000000001', 'APPROVED', ?, ?, 'discord:100000000000000000' WHERE NOT EXISTS (SELECT 1 FROM gentool_user_link WHERE player_id = 'D3A000000001')",
            time, time,
        )
        updated += jdbc.update(
            "INSERT INTO replay_collection_job (id, trigger_type, status, start_date, end_date, requested_by, created_at, started_at, finished_at, directories_discovered, directories_scanned, files_discovered, files_imported, files_skipped, failures) SELECT 'demo-collection-completed', 'MANUAL', 'COMPLETED', '2026-11-03', '2026-11-07', 'discord:100000000000000000', ?, ?, ?, 15, 15, 50, 50, 0, 0 WHERE NOT EXISTS (SELECT 1 FROM replay_collection_job WHERE id = 'demo-collection-completed')",
            time, time, time,
        )
        return updated
    }
}
