# Deployment

## Build images

Run the image build from the repository root. Images are published under the fixed `generaltao725` Docker Hub namespace:

```bash
mvn -pl backend -am -P build-docker-image package -DskipTests; docker build -t generaltao725/gentool-data-viewer-frontend:latest frontend

```

Register both backend callback URLs in the Discord application under **OAuth2 > Redirects**:

- `https://gentool-data-viewer-api-stage.taonity.org/login/oauth2/code/discord-gentool-data-viewer`
- `https://gentool-data-viewer-api.taonity.org/login/oauth2/code/discord-gentool-data-viewer`

## Docker Compose

Run the local stack from the repository root with the tracked test environment and published ports:

```bash
docker compose --env-file templates/docker/.env.test -f templates/docker/docker-compose.yml -f templates/docker/docker-compose.ports-local.yml up

```

For production, create an environment file with deployment-specific values:

```env
COMPOSE_PROJECT_NAME=gentool-data-viewer-prod

POSTGRES_USER=dbadmin
POSTGRES_PASSWORD=strong-password
POSTGRES_APP_USER=app
POSTGRES_APP_PASSWORD=app-password
DISCORD_CLIENT_ID=your-real-client-id
DISCORD_CLIENT_SECRET=your-real-secret
DISCORD_BOT_TOKEN=your-discord-bot-token
CONSOLE_OWNER_DISCORD_IDS=your-numeric-discord-user-id
CONSOLE_ADMIN_DISCORD_IDS=
SPRING_PROFILES_ACTIVE=prod
FRONTEND_PROFILE=prod

```

The environment file contains Compose bootstrap selectors, credentials, and the backend/frontend profiles. `DISCORD_BOT_TOKEN` enables admins to resolve users who have not logged in; create it for a Discord bot application and keep it server-side. Use `COMPOSE_PROJECT_NAME=gentool-data-viewer-stage`, `SPRING_PROFILES_ACTIVE=stage`, and `FRONTEND_PROFILE=stage` for staging. Public URLs, cookie names, database topology, and internal service addresses remain committed in application profiles and Compose configuration.

The backend defaults to a 768 MiB container limit, with an initial heap target of 10% and a maximum heap of 60% of that limit. Override `BACKEND_MEMORY_LIMIT`, `BACKEND_INITIAL_RAM_PERCENTAGE`, or `BACKEND_MAX_RAM_PERCENTAGE` in the deployment environment only after checking container memory and GC metrics under representative load. To attach a debugger temporarily, set `BACKEND_JAVA_EXTRA_OPTIONS` to the required JDWP agent options.

The file is passed with Compose's `--env-file` option for interpolation. Services receive only their explicitly listed variables, so database and OAuth credentials are not injected into the frontend container.

The production override [`docker-compose.prodenv.yml`](../templates/docker/docker-compose.prodenv.yml) attaches backend and frontend to the external `prodenv-shared-internal` network. That network must already exist in the target production environment.

Start production Compose from the repository root:

```bash
docker compose --env-file templates/docker/.env -f templates/docker/docker-compose.yml -f templates/docker/docker-compose.prodenv.yml up -d

```

## Health endpoints

- Backend: `GET /actuator/health`
- Backend liveness: `GET /actuator/health/liveness`
- Backend readiness: `GET /actuator/health/readiness`
- Frontend proxy: `GET /api/actuator/health`

## Conventions

- Keep configurable values in `application*.yaml`, frontend environment files, or Compose; do not hardcode environment defaults in application code.
- Keep backend and frontend cookie names aligned, and use a shared parent cookie domain when they run on different subdomains.
- Add every infrastructure service to the base Compose file with a production-ready configuration and a local published-port override when developers need direct access.
- Never commit production credentials or deployment environment files.
