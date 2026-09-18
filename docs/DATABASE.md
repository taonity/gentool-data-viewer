# Database Migrations

This project uses Flyway for database schema management.

## Location

SQL migrations live in `templates/docker/flyway/sql/tables/`.

## Naming Convention

```ini
V{version}__{description}.sql

```

- Version numbers start at `100000` and increment
- Use double underscore `__` between version and description
- Description uses snake_case

Examples:

```ini
V100000__create_user_table.sql
V100001__add_profile_columns.sql
V100002__create_orders_table.sql

```

## Local Development (H2)

When running with the `h2` profile, Flyway reads migrations from `filesystem:templates/docker/flyway/sql/tables/`. The database is automatically cleaned and rebuilt on each start (via `flyway-clean-migrate` profile).

Run from the project root:

```bash
mvn -pl backend spring-boot:run '-Dspring-boot.run.jvmArguments="-Dspring.profiles.active=h2,stub-discord,local"'

```

## Production (PostgreSQL)

In production, Flyway runs as a separate Docker container before the app starts (see `templates/docker/flyway/docker-compose.yml`). Migrations are never rolled back — write forward-only migrations.

## Writing Migrations

### Create a table

```sql
-- V100001__create_orders_table.sql
CREATE TABLE orders (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     VARCHAR NOT NULL REFERENCES app_user(google_id),
    total       DECIMAL(10,2) NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT now()
);

```

### Add a column

```sql
-- V100002__add_order_status.sql
ALTER TABLE orders ADD COLUMN status VARCHAR NOT NULL DEFAULT 'pending';

```

### H2 Compatibility

H2 is mostly PostgreSQL-compatible. If you need H2-specific SQL, create a `flyway/sql/conflicts/h2/` directory and add the H2 variant there. Configure in `application-h2.yaml`:

```yaml
spring:
  flyway:
    locations:
      - filesystem:templates/docker/flyway/sql/tables
      - filesystem:templates/docker/flyway/sql/conflicts/h2

```

## Match Date Periods

Players and Replays accept optional `startDate` and `endDate` in `YYYY-MM-DD` form.
Both use replay `match_at`, not collection or source-directory dates. Start is
inclusive UTC midnight; end includes the entire UTC calendar day. Either bound
may be omitted. Omitting both keeps the existing all-time query path.

Both date filters display the earliest and latest replay match dates by default,
obtained from `/console/replays/date-range` in UTC. The All time selection still
omits date parameters so newly collected data is included automatically. Clearing
a custom period restores these visible full-range dates. An empty replay dataset
returns null bounds instead of inventing dates.

The period player view is read-only: names are ranked by frequency, latest use,
then name; Games counts reports by reporter ID; CPU and lineup come from the latest
report inside the period. Players without period reports are excluded, including
pinned players. Sorting, pagination, pinned rank, and player summary counts use
the period dataset. Discord links and GenTool collection timestamps remain current.
Historical CPU observations are matched against the current benchmark catalog,
not a historical benchmark-score snapshot.

SQL reduces history to one row per player/name; the service rates distinct CPUs
and filters/sorts those aggregate rows before pagination. This avoids loading raw
reports but memory use still scales with active players and aliases in the period.
The `(reporter_id, match_at)` index supports player history access; `match_at` is
also indexed for date-range scans. Benchmark larger production datasets before
adding caching or precomputed daily aggregates.

## Tips

- Keep migrations small and focused
- Never modify an existing migration that's been applied in production
- Test migrations locally with H2 before deploying
- Use `flyway.conf` for Flyway container configuration
