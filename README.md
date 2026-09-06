# Gentool Data Viewer

A full-stack viewer and collector for GenTool replay data with Discord OAuth2 authentication.

## What is included

- Kotlin and Spring Boot 4 backend with session authentication and CSRF protection
- Next.js TypeScript frontend with backend calls proxied through Next.js API routes
- Discord OAuth2 production and WireMock-backed local login profiles
- PostgreSQL, H2, Flyway, health endpoints, and Docker Compose deployment templates

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

## Guides

- [Add a feature](docs/ADD_FEATURE.md)
- [Integrate an external API](docs/ADD_EXTERNAL_API.md)
- [Add or change an OAuth2 provider](docs/ADD_OAUTH2_PROVIDER.md)
- [Database and migrations](docs/DATABASE.md)
- [Testing](docs/TESTING.md)
- [Deployment](docs/DEPLOYMENT.md)

replace google with discrod auth
make it possible to force update with tokens available
figure out what to do with unrated cpus
all of the columns shoud be searchable
strange delays during tab switch
