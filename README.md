# Gentool Data Viewer

A full-stack viewer and collector for GenTool replay data with Discord OAuth2 authentication.

## What is included

- Kotlin and Spring Boot 4 backend with session authentication and CSRF protection
- Next.js TypeScript frontend with backend calls proxied through Next.js API routes
- Discord OAuth2 production and WireMock-backed local login profiles
- PostgreSQL, H2, Flyway, health endpoints, and Docker Compose deployment templates

## Discord application review

The public legal documents required for the Discord application are available without authentication:

- Privacy Policy: `https://gentool-data-viewer.taonity.org/privacy`
- Terms of Service: `https://gentool-data-viewer.taonity.org/terms`

Enter these URLs in the Discord Developer Portal under the application's **General Information** page. Before requesting review, verify that both production URLs return `200`, keep the application description and requested `identify` scope accurate, and ensure `taonity.org@gmail.com` remains a monitored support, privacy, and data-deletion channel. Update the documents whenever collected data, Discord scopes, providers, retention, or user-facing functionality changes.

## Prerequisites

- Java 17
- Maven
- Node.js 24 and npm
- Docker and Docker Compose (optional)

## Profiles

Choose one profile from each resource group.

| Resource | Local/stub | Production |
|---|---|---|
| Database | `h2` | `postgres` |
| OAuth2 | `stub-discord` | `prod-discord` |
| Logging | `plain-log` (included by `local`) | default |
| General | `local` | none |

Local development uses `h2,stub-discord,local`. Stage and production use only `stage` or `prod`; each environment profile includes `postgres` and `prod-discord` and owns its non-secret deployment settings. Add the optional `demo-data` profile to seed local feature fixtures, including 50 anonymized replays and 15 fictional players with CPU ratings.

## Self-service rescans

Authenticated VIEWER users claim or replace their linked GenTool identity directly from a Players row; no admin approval is required. Players rows provide targeted refresh actions, and Replays rows can refresh that replay reporter. The linked player does not consume the daily count quota; other-player refreshes are limited to 20 per UTC day. All refreshes use the same serial, throttled collector queue, scan the configured seven-day lookback, enforce a five-minute per-target cooldown, and are recorded in the rescan ledger and audit log.

## CPU ratings

CPU ratings use PassMark single-thread scores. Matching normalizes known brand-string formatting, including AMD Radeon and core-count suffixes, Intel Core 2 names, legacy Intel mobile names, compact Xeon names, and truncated clock suffixes. Model variants such as PRO, G/GE, H/HS/HX, X3D, and Xeon versions remain distinct. Multiple matching catalog records remain ambiguous; incomplete model names and motherboard/chipset descriptions are not guessed.

After deploying changes to CPU matching, an administrator must use **Refresh benchmarks** in the CPU players view (or wait for the configured scheduled refresh). This rebuilds the stored catalog keys and rerates existing player hardware; restarting alone does not update stored scores. Compare rated, unmatched, ambiguous, and missing-CPU counts after the refresh before treating remaining unrated CPUs as missing benchmarks. No secondary benchmark scores or estimates are mixed into the ranking.

## Run locally

Run each command from the repository root.

Start the backend:

```bash
mvn -pl backend spring-boot:run '-Dspring-boot.run.jvmArguments="-Dspring.profiles.active=h2,stub-discord,local"'
```

Start the backend with pending access requests, audit records, and anonymized replay/player fixtures:

```bash
mvn -pl backend spring-boot:run '-Dspring-boot.run.jvmArguments="-Dspring.profiles.active=h2,stub-discord,local,demo-data"'
```

Start the frontend in another terminal:

```bash
npm install --prefix frontend; npm run dev --prefix frontend
```

The backend runs on `http://127.0.0.1:8080`; the frontend runs on `http://127.0.0.1:3000`.

Build the backend container image from the repository root:

```bash
mvn clean install -P build-docker-image -DskipTests
```

Build the frontend container image from the repository root:

```bash
docker build -t generaltao725/gentool-data-viewer-frontend:latest frontend
```

For the complete local stack with published images:

```bash
docker compose --env-file templates/docker/.env.test -f templates/docker/docker-compose.yml -f templates/docker/docker-compose.ports-local.yml up
```

See [Deployment](docs/DEPLOYMENT.md) for production configuration and Compose requirements. See [Database](docs/DATABASE.md) for schema and migration management.

## Database Schema

<!-- mermerd-start -->
```mermaid
%%{init: {"theme":"base","look":"classic","htmlLabels":true,"fontFamily":"Verdana, sans-serif","layout":"dagre","themeVariables":{"fontSize":"12px","primaryColor":"#ffffff","primaryBorderColor":"#bcc9d8","primaryTextColor":"#20344d","lineColor":"#7196bd","edgeLabelBackground":"transparent"},"themeCSS":".schema-table{border-collapse:collapse;white-space:nowrap;font:12px/1.3 Verdana,sans-serif;color:light-dark(rgb(32 52 77),rgb(220 229 241));background-color:light-dark(rgb(255 255 255),rgb(24 32 43));border:1px solid light-dark(rgb(188 201 216),rgb(82 97 116));}.schema-table th,.schema-table td{font:12px/1.3 Verdana,sans-serif;}.schema-table th{padding:3px 6px;text-align:center;font-weight:600;background-color:light-dark(rgb(232 238 245),rgb(42 53 68));}.schema-table td{padding:2px 6px;text-align:left;border-top:1px solid light-dark(rgb(229 235 242),rgb(52 65 83));}.schema-table .typeText{background-color:light-dark(rgb(220 245 239),rgb(23 61 56));color:light-dark(rgb(15 118 110),rgb(167 223 210));}.schema-table .typeNumber{background-color:light-dark(rgb(225 237 255),rgb(31 53 87));color:light-dark(rgb(29 78 216),rgb(182 206 255));}.schema-table .typeDateTime{background-color:light-dark(rgb(238 229 255),rgb(56 42 80));color:light-dark(rgb(126 34 206),rgb(217 186 243));}.schema-table .typeBoolean{background-color:light-dark(rgb(255 240 207),rgb(68 54 30));color:light-dark(rgb(146 64 14),rgb(242 208 140));}.schema-table .typeBinary{background-color:light-dark(rgb(252 229 239),rgb(72 38 55));color:light-dark(rgb(190 24 93),rgb(240 186 208));}.schema-table .typeStructured{background-color:light-dark(rgb(232 231 255),rgb(44 43 80));color:light-dark(rgb(67 56 202),rgb(198 195 250));}.schema-table .typeOther{background-color:light-dark(rgb(224 243 252),rgb(34 60 73));color:light-dark(rgb(20 100 126),rgb(176 221 235));}.schema-table .primaryKey{background-color:light-dark(rgb(255 240 207),rgb(68 54 30));color:light-dark(rgb(146 64 14),rgb(242 208 140));}.schema-table .foreignKey{background-color:light-dark(rgb(225 237 255),rgb(31 53 87));color:light-dark(rgb(29 78 216),rgb(182 206 255));}.schema-table .uniqueKey{background-color:light-dark(rgb(238 229 255),rgb(56 42 80));color:light-dark(rgb(126 34 206),rgb(217 186 243));}.schema-edge{display:inline-block;padding:2px 4px;text-align:left;font:12px/1.3 Verdana,sans-serif;color:light-dark(rgb(32 52 77),rgb(220 229 241));background:light-dark(rgb(232 238 245),rgb(42 53 68));}.schema-table,.schema-edge{color-scheme:light dark;}@scope ([data-color-mode=dark],.vscode-dark,.vscode-high-contrast,[data-theme=dark]){.schema-table,.schema-edge{color-scheme:dark;}}@scope ([data-color-mode=light],.vscode-light,.vscode-high-contrast-light,[data-theme=light]){.schema-table,.schema-edge{color-scheme:light;}}","flowchart":{"padding":0.01,"wrappingWidth":100000,"rankSpacing":24,"nodeSpacing":18,"curve":"linear","subGraphTitleMargin":{"top":0,"bottom":0}}}}%%
flowchart TB
subgraph row_0[" "]
direction LR
table_0["<table class='schema-table'><thead><tr><th colspan='3'>app_user</th></tr><tr><th>Column name</th><th>Data type</th><th>Constraints</th></tr></thead><tbody><tr><td>user_id</td><td class='typeText'>varchar</td><td class='primaryKey'>PK</td></tr><tr><td>display_name</td><td class='typeText'>varchar</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>picture_url</td><td class='typeText'>varchar</td><td class='plainConstraint'>—</td></tr><tr><td>role</td><td class='typeText'>varchar(20)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>access_status</td><td class='typeText'>varchar(20)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>requested_role</td><td class='typeText'>varchar(20)</td><td class='plainConstraint'>—</td></tr><tr><td>auth_provider</td><td class='typeText'>varchar(20)</td><td class='plainConstraint'>NOT NULL</td></tr></tbody></table>"]
table_4["<table class='schema-table'><thead><tr><th colspan='3'>gentool_user_link</th></tr><tr><th>Column name</th><th>Data type</th><th>Constraints</th></tr></thead><tbody><tr><td>user_id</td><td class='typeText'>varchar</td><td class='primaryKey'>PK, FK</td></tr><tr><td>player_id</td><td class='typeText'>varchar(12)</td><td class='primaryKey'>PK, UK</td></tr><tr><td>decided_by_user_id</td><td class='typeText'>varchar</td><td class='foreignKey'>FK</td></tr><tr><td>status</td><td class='typeText'>varchar(20)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>requested_at</td><td class='typeDateTime'>timestamp</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>decided_at</td><td class='typeDateTime'>timestamp</td><td class='plainConstraint'>—</td></tr></tbody></table>"]
table_10["<table class='schema-table'><thead><tr><th colspan='3'>replay_rescan_request</th></tr><tr><th>Column name</th><th>Data type</th><th>Constraints</th></tr></thead><tbody><tr><td>id</td><td class='typeText'>varchar(36)</td><td class='primaryKey'>PK</td></tr><tr><td>requested_by_user_id</td><td class='typeText'>varchar</td><td class='foreignKey'>FK · NOT NULL</td></tr><tr><td>job_id</td><td class='typeText'>varchar(36)</td><td class='foreignKey'>FK · NOT NULL</td></tr><tr><td>target_player_id</td><td class='typeText'>varchar(12)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>own_target</td><td class='typeBoolean'>bool</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>requested_at</td><td class='typeDateTime'>timestamp</td><td class='plainConstraint'>NOT NULL</td></tr></tbody></table>"]
table_4 ~~~ table_10
end
style row_0 fill:transparent,stroke:transparent
subgraph row_1[" "]
direction LR
table_8["<table class='schema-table'><thead><tr><th colspan='3'>replay_collection_job</th></tr><tr><th>Column name</th><th>Data type</th><th>Constraints</th></tr></thead><tbody><tr><td>id</td><td class='typeText'>varchar(36)</td><td class='primaryKey'>PK</td></tr><tr><td>trigger_type</td><td class='typeText'>varchar(20)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>status</td><td class='typeText'>varchar(20)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>start_date</td><td class='typeDateTime'>date</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>end_date</td><td class='typeDateTime'>date</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>requested_by</td><td class='typeText'>varchar(320)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>created_at</td><td class='typeDateTime'>timestamp</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>started_at</td><td class='typeDateTime'>timestamp</td><td class='plainConstraint'>—</td></tr><tr><td>finished_at</td><td class='typeDateTime'>timestamp</td><td class='plainConstraint'>—</td></tr><tr><td>directories_discovered</td><td class='typeNumber'>bigint</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>directories_scanned</td><td class='typeNumber'>bigint</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>files_discovered</td><td class='typeNumber'>bigint</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>files_imported</td><td class='typeNumber'>bigint</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>files_skipped</td><td class='typeNumber'>bigint</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>failures</td><td class='typeNumber'>bigint</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>error_message</td><td class='typeText'>varchar(2000)</td><td class='plainConstraint'>—</td></tr><tr><td>user_limit</td><td class='typeNumber'>int</td><td class='plainConstraint'>—</td></tr><tr><td>target_player_id</td><td class='typeText'>varchar(12)</td><td class='plainConstraint'>—</td></tr></tbody></table>"]
table_1["<table class='schema-table'><thead><tr><th colspan='3'>audit_log</th></tr><tr><th>Column name</th><th>Data type</th><th>Constraints</th></tr></thead><tbody><tr><td>id</td><td class='typeText'>varchar(36)</td><td class='primaryKey'>PK</td></tr><tr><td>action</td><td class='typeText'>varchar(40)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>target_type</td><td class='typeText'>varchar(40)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>target_id</td><td class='typeText'>varchar(64)</td><td class='plainConstraint'>—</td></tr><tr><td>actor_user_id</td><td class='typeText'>varchar</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>occurred_at</td><td class='typeDateTime'>timestamp</td><td class='plainConstraint'>NOT NULL</td></tr></tbody></table>"]
table_2["<table class='schema-table'><thead><tr><th colspan='3'>config_override</th></tr><tr><th>Column name</th><th>Data type</th><th>Constraints</th></tr></thead><tbody><tr><td>config_key</td><td class='typeText'>varchar(200)</td><td class='primaryKey'>PK</td></tr><tr><td>value_json</td><td class='typeText'>varchar(20000)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>updated_at</td><td class='typeDateTime'>timestamp</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>updated_by</td><td class='typeText'>varchar(320)</td><td class='plainConstraint'>NOT NULL</td></tr></tbody></table>"]
table_8 ~~~ table_1
table_1 ~~~ table_2
end
style row_1 fill:transparent,stroke:transparent
row_0 ~~~ row_1
subgraph row_2[" "]
direction LR
table_3["<table class='schema-table'><thead><tr><th colspan='3'>cpu_benchmark</th></tr><tr><th>Column name</th><th>Data type</th><th>Constraints</th></tr></thead><tbody><tr><td>source_id</td><td class='typeText'>varchar(32)</td><td class='primaryKey'>PK</td></tr><tr><td>model_name</td><td class='typeText'>varchar(500)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>normalized_name</td><td class='typeText'>varchar(500)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>normalized_model</td><td class='typeText'>varchar(500)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>single_thread_score</td><td class='typeNumber'>int</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>source_url</td><td class='typeText'>varchar(1000)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>fetched_at</td><td class='typeDateTime'>timestamp</td><td class='plainConstraint'>NOT NULL</td></tr></tbody></table>"]
end
style row_2 fill:transparent,stroke:transparent
row_1 ~~~ row_2
subgraph row_3[" "]
direction LR
table_5["<table class='schema-table'><thead><tr><th colspan='3'>player_hardware</th></tr><tr><th>Column name</th><th>Data type</th><th>Constraints</th></tr></thead><tbody><tr><td>player_id</td><td class='typeText'>varchar(64)</td><td class='primaryKey'>PK</td></tr><tr><td>source_replay_id</td><td class='typeText'>varchar(36)</td><td class='foreignKey'>FK · NOT NULL</td></tr><tr><td>latest_name</td><td class='typeText'>varchar(255)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>cpu</td><td class='typeText'>varchar(500)</td><td class='plainConstraint'>—</td></tr><tr><td>cpu_score</td><td class='typeNumber'>int</td><td class='plainConstraint'>—</td></tr><tr><td>system_info</td><td class='typeText'>varchar(10000)</td><td class='plainConstraint'>—</td></tr><tr><td>observed_at</td><td class='typeDateTime'>timestamp</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>cpu_benchmark_id</td><td class='typeText'>varchar(32)</td><td class='plainConstraint'>—</td></tr><tr><td>cpu_benchmark_name</td><td class='typeText'>varchar(500)</td><td class='plainConstraint'>—</td></tr><tr><td>cpu_benchmark_url</td><td class='typeText'>varchar(1000)</td><td class='plainConstraint'>—</td></tr><tr><td>cpu_match_status</td><td class='typeText'>varchar(20)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>cpu_score_updated_at</td><td class='typeDateTime'>timestamp</td><td class='plainConstraint'>—</td></tr><tr><td>main_name</td><td class='typeText'>varchar(255)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>aliases_json</td><td class='typeText'>varchar(5000)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>replay_count</td><td class='typeNumber'>bigint</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>gentool_updated_at</td><td class='typeDateTime'>timestamp</td><td class='plainConstraint'>—</td></tr></tbody></table>"]
table_6["<table class='schema-table'><thead><tr><th colspan='3'>replay</th></tr><tr><th>Column name</th><th>Data type</th><th>Constraints</th></tr></thead><tbody><tr><td>id</td><td class='typeText'>varchar(36)</td><td class='primaryKey'>PK</td></tr><tr><td>source_url</td><td class='typeText'>varchar(2000)</td><td class='uniqueKey'>UK · NOT NULL</td></tr><tr><td>source_date</td><td class='typeDateTime'>date</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>reporter_id</td><td class='typeText'>varchar(64)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>reporter_name</td><td class='typeText'>varchar(255)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>match_at</td><td class='typeDateTime'>timestamp</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>gentool_version</td><td class='typeText'>varchar(50)</td><td class='plainConstraint'>—</td></tr><tr><td>game_version</td><td class='typeText'>varchar(255)</td><td class='plainConstraint'>—</td></tr><tr><td>install_type</td><td class='typeText'>varchar(1000)</td><td class='plainConstraint'>—</td></tr><tr><td>map_name</td><td class='typeText'>varchar(500)</td><td class='plainConstraint'>—</td></tr><tr><td>start_cash</td><td class='typeNumber'>int</td><td class='plainConstraint'>—</td></tr><tr><td>match_type</td><td class='typeText'>varchar(50)</td><td class='plainConstraint'>—</td></tr><tr><td>match_length_seconds</td><td class='typeNumber'>bigint</td><td class='plainConstraint'>—</td></tr><tr><td>match_mode</td><td class='typeText'>varchar(50)</td><td class='plainConstraint'>—</td></tr><tr><td>system_info</td><td class='typeText'>varchar(10000)</td><td class='plainConstraint'>—</td></tr><tr><td>cpu</td><td class='typeText'>varchar(500)</td><td class='plainConstraint'>—</td></tr><tr><td>replay_file_name</td><td class='typeText'>varchar(1000)</td><td class='plainConstraint'>—</td></tr><tr><td>replay_size_bytes</td><td class='typeNumber'>bigint</td><td class='plainConstraint'>—</td></tr><tr><td>collected_at</td><td class='typeDateTime'>timestamp</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>windows_compat</td><td class='typeText'>varchar(255)</td><td class='plainConstraint'>—</td></tr><tr><td>rep_info_in_use</td><td class='typeText'>varchar(50)</td><td class='plainConstraint'>—</td></tr><tr><td>fields_json</td><td class='typeText'>varchar(30000)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>raw_text</td><td class='typeText'>varchar(100000)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>player_names</td><td class='typeText'>varchar(2000)</td><td class='plainConstraint'>NOT NULL</td></tr></tbody></table>"]
table_7["<table class='schema-table'><thead><tr><th colspan='3'>replay_associated_file</th></tr><tr><th>Column name</th><th>Data type</th><th>Constraints</th></tr></thead><tbody><tr><td>id</td><td class='typeText'>varchar(36)</td><td class='primaryKey'>PK</td></tr><tr><td>replay_id</td><td class='typeText'>varchar(36)</td><td class='foreignKey'>FK · NOT NULL</td></tr><tr><td>file_name</td><td class='typeText'>varchar(1000)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>size_bytes</td><td class='typeNumber'>bigint</td><td class='plainConstraint'>NOT NULL</td></tr></tbody></table>"]
end
style row_3 fill:transparent,stroke:transparent
row_2 ~~~ row_3
subgraph row_4[" "]
direction LR
table_9["<table class='schema-table'><thead><tr><th colspan='3'>replay_player</th></tr><tr><th>Column name</th><th>Data type</th><th>Constraints</th></tr></thead><tbody><tr><td>id</td><td class='typeText'>varchar(36)</td><td class='primaryKey'>PK</td></tr><tr><td>replay_id</td><td class='typeText'>varchar(36)</td><td class='foreignKey'>FK · NOT NULL</td></tr><tr><td>team_number</td><td class='typeNumber'>int</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>slot_number</td><td class='typeNumber'>int</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>address</td><td class='typeText'>varchar(32)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>name</td><td class='typeText'>varchar(255)</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>army</td><td class='typeText'>varchar(255)</td><td class='plainConstraint'>—</td></tr></tbody></table>"]
table_11["<table class='schema-table'><thead><tr><th colspan='3'>spring_session</th></tr><tr><th>Column name</th><th>Data type</th><th>Constraints</th></tr></thead><tbody><tr><td>primary_id</td><td class='typeText'>char(36)</td><td class='primaryKey'>PK</td></tr><tr><td>session_id</td><td class='typeText'>char(36)</td><td class='uniqueKey'>UK · NOT NULL</td></tr><tr><td>creation_time</td><td class='typeNumber'>bigint</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>last_access_time</td><td class='typeNumber'>bigint</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>max_inactive_interval</td><td class='typeNumber'>int</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>expiry_time</td><td class='typeNumber'>bigint</td><td class='plainConstraint'>NOT NULL</td></tr><tr><td>principal_name</td><td class='typeText'>varchar(100)</td><td class='plainConstraint'>—</td></tr></tbody></table>"]
table_12["<table class='schema-table'><thead><tr><th colspan='3'>spring_session_attributes</th></tr><tr><th>Column name</th><th>Data type</th><th>Constraints</th></tr></thead><tbody><tr><td>session_primary_id</td><td class='typeText'>char(36)</td><td class='primaryKey'>PK, FK</td></tr><tr><td>attribute_name</td><td class='typeText'>varchar(200)</td><td class='primaryKey'>PK</td></tr><tr><td>attribute_bytes</td><td class='typeBinary'>bytea</td><td class='plainConstraint'>NOT NULL</td></tr></tbody></table>"]
table_9 ~~~ table_11
end
style row_4 fill:transparent,stroke:transparent
row_3 ~~~ row_4
table_12 -->|"<span class='schema-edge'>session_primary_id<br/>0..* → 1</span>"| table_11
table_9 -.->|"<span class='schema-edge'>replay_id<br/>0..* → 1</span>"| table_6
table_5 -.->|"<span class='schema-edge'>source_replay_id<br/>0..* → 1</span>"| table_6
table_7 -.->|"<span class='schema-edge'>replay_id<br/>0..* → 1</span>"| table_6
table_10 -.->|"<span class='schema-edge'>requested_by_user_id<br/>0..* → 1</span>"| table_0
table_10 -.->|"<span class='schema-edge'>job_id<br/>0..* → 1</span>"| table_8
table_4 -.->|"<span class='schema-edge'>decided_by_user_id<br/>0..* → 0..1</span>"| table_0
table_4 -->|"<span class='schema-edge'>user_id<br/>0..* → 1</span>"| table_0
classDef schemaNode fill:transparent,stroke:transparent
class table_0,table_1,table_2,table_3,table_4,table_5,table_6,table_7,table_8,table_9,table_10,table_11,table_12 schemaNode
```
<!-- mermerd-end -->

## Guides

- [Add a feature](docs/ADD_FEATURE.md)
- [Integrate an external API](docs/ADD_EXTERNAL_API.md)
- [Add or change an OAuth2 provider](docs/ADD_OAUTH2_PROVIDER.md)
- [Database and migrations](docs/DATABASE.md)
- [Testing](docs/TESTING.md)
- [Deployment](docs/DEPLOYMENT.md)

## TODO

- [ ] Decide how to handle unrated CPUs
