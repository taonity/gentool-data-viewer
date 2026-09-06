# AGENTS.md

## Architecture

Multi-module Maven monorepo (Spring Boot 4 / Kotlin backend + Next.js TypeScript frontend).

- **`backend/`** — Main backend. Kotlin, Spring Boot 4, JPA/Hibernate, Spring Security OAuth2 (Discord login). Package-per-feature layout under `org.taonity.gentooldataviewer`: `common/`, `config/`, `console/`, `security/`, `user/`, and supporting infrastructure packages.
- **`discord-stubs/`** — WireMock stubs for Discord OAuth2 (resources under `src/main/resources/wiremock/discord/`). Used by the `stub-discord` profile for local development without real Discord credentials.
- **`frontend/`** — Next.js app (`src/app/` App Router). All backend calls go through Next.js API routes (`src/app/api/`) which proxy to the backend via `src/lib/backend.ts` using `fetchFromBackend()`.
- **`templates/docker/`** — Docker Compose templates with Flyway migrations in `flyway/sql/tables/`.

### Key data flow

1. User logs in via Discord OAuth2 → `security/service/OAuth2UserPersistenceService` persists/updates the provider-namespaced user.
2. Frontend calls `/api/console/access/me` → Next.js route → backend `/console/access/me` → returns the current user's console access info.
3. All API calls are proxied through Next.js API routes (never call backend directly from client).

## Build & Run

Run each command from the repository root.

Full build:

```bash
mvn clean install

```

Run backend locally:

```bash
mvn -pl backend spring-boot:run '-Dspring-boot.run.jvmArguments="-Dspring.profiles.active=h2,stub-discord,local"'

```

Run frontend:

```bash
npm ci --prefix frontend; npm run dev --prefix frontend

```

Backend runs on port **8080**, frontend on **3000**.

### Profile system (one per resource group)

| Resource | Local/stub       | Production     |
|----------|------------------|----------------|
| DB       | `h2`             | `postgres`     |
| OAuth2   | `stub-discord`   | `prod-discord` |
| Logging  | `plain-log`      | *(default)*    |
| General  | `local`          | *(none)*       |

Local set: `h2,stub-discord,local` (`local` auto-includes `plain-log`). Add `demo-data` for local feature fixtures. Production set: `postgres,prod-discord`. Never activate `demo-data` in production. Stubs use WireMock (classpath mode).

## Conventions & Patterns

- **Package-per-feature**: each feature has `controller/`, `service/`, `dto/`, `entity/`, `repository/`, `exception/` sub-packages. Follow this layout when adding features.
- **Logging**: use `io.github.oshai.kotlinlogging.KotlinLogging` (`private val LOGGER = KotlinLogging.logger {}`), placed in a `companion object`.
- **Controller pattern**: `@RestController`, inject services, use `@AuthenticationPrincipal principal: AuthenticatedUserPrincipal` for auth. `ControllerLoggingInterceptor` automatically logs every controller method invocation.
- **CSRF**: SPA pattern with `CookieCsrfTokenRepository` + `SpaCsrfTokenRequestHandler`. Frontend reads CSRF cookie and sends `X-XSRF-TOKEN` header on mutating requests.
- **Frontend API proxy**: every backend call is proxied through Next.js API routes in `src/app/api/`. Never call the backend directly from client components.
- __DB migrations__: Flyway SQL scripts in `templates/docker/flyway/sql/tables/` (naming: `V100000__description.sql`). H2 profile uses Flyway with `filesystem:` locations.
- **Demo data**: when a persistent feature benefits from local examples, add an idempotent `DemoDataContributor` in that feature package under `@Profile("demo-data")`. Use deterministic markers, never overwrite existing records, and extend `DemoDataProfileTest`.

## Testing

- **MVC integration tests**: use `@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("h2")` with `oauth2Login()` mock.
- **Authenticated browser checks**: protected frontend functionality requires a session. If Playwright or another browser tool lands on `/login` or receives `401`, open **Developer tools** using the bottom-right bug button (or `Alt+Shift+D`), choose **Owner (test@example.com)**, wait for the redirect to `/`, and only then inspect the protected UI. Reuse that authenticated browser page for subsequent checks.
- Do not report `/login`, missing protected elements, or pre-login `401` responses as application failures until the dev Owner shortcut has been used.
- Run tests from the repository root with `mvn test`.

## Adding Features

See `docs/ADD_FEATURE.md` for the step-by-step guide.
