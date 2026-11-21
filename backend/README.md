# NeuroGuard Backend

Spring Boot 3.5 / Java 21 / PostgreSQL 16 / Flyway. Status: project setup + data model (v0.2.0). No business endpoints or auth yet.

## Prerequisites
- JDK 21
- Docker (for local Postgres and for the Testcontainers test)

## Team database (shared, hosted on Neon)
The team shares ONE hosted Postgres, so everyone sees the same data. You do not need Docker for this.

1. Ask the backend owner for the credentials (privately: password manager or DM, never in git, issues or group chats).
2. `cp .env.example .env` in `backend/` and fill in `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`.
   `.env` is git-ignored. Never commit it.
3. `./mvnw spring-boot:run`. Flyway applies any new migrations to the shared DB automatically.

Rules for the shared DB:
- Schema changes only through a new `src/main/resources/db/migration/V<n>__description.sql`. Never edit an applied migration (Flyway will refuse to start for everyone). Tell the team before pushing one, and renumber if two people create the same version.
- Do not store real patient data. Prefix test records (e.g. name "TEST ...") so they are easy to clean up.
- To browse data, use the Neon console (Tables tab) or a client like DBeaver with the same credentials (host, port 5432, database, user, password, SSL required).
- If the password leaks: reset it in Neon (Connection details > Reset password) and send the new one to the team.

## Run locally with your own throwaway database (optional)
Leave `DB_URL` out of `.env` and use Docker:
```bash
cd backend
docker compose up -d          # Postgres 16 on localhost:5432
./mvnw spring-boot:run        # starts on http://localhost:8080 (profile "dev" is the default)
```
Flyway applies `V1__init_schema.sql` on startup and Hibernate validates the entities against it
(`ddl-auto=validate`). Schema changes go in new `V<n>__*.sql` files; never edit an applied migration.

## Tests
```bash
./mvnw test
```
`ApplicationStartupTest` boots the whole app on a throwaway Postgres via Testcontainers (skipped if Docker is not running).

## Environment variables
All have dev defaults; override them for any non-local environment (do not commit real secrets).

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/neuroguard` | JDBC URL |
| `DB_USERNAME` | `neuroguard` | DB user (also used by docker-compose) |
| `DB_PASSWORD` | `neuroguard` | DB password (also used by docker-compose) |
| `DB_NAME`, `DB_PORT` | `neuroguard`, `5432` | docker-compose only |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated allowed origins |

See `.env.example`.

## API docs
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

The Authorize button takes a bearer JWT (JWT issuing arrives in the next task).

## Errors
Every error uses one shape:
```json
{ "timestamp": "...", "status": 401, "error": "Unauthorized", "message": "...", "path": "/api/...", "fieldErrors": [ { "field": "...", "message": "..." } ] }
```
Throw `ResourceNotFoundException` (404) or `ConflictException` (409) from services.

## Layout
`com.neuroguard.{config, common, patient, staff, appointment, clinical, billing, imaging, scanner, audit}`;
each module has `entity/`, `repository/` and placeholder `service/`, `controller/`, `dto/` packages.
Notes: the `service` table maps to `ServiceItem`; `audit_log.actor_id` is polymorphic (staff or patient) so it has no FK.
