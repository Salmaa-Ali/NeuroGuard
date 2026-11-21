# NeuroGuard Backend

Spring Boot 3.5 / Java 21 / PostgreSQL 16 / Flyway. Status: data model, JWT authentication, role/scope authorization and admin staff endpoints. Permission rules: [`docs/permissions.md`](../docs/permissions.md).

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
**Docker Desktop must be running.** The integration tests start a throwaway Postgres 16 with Testcontainers
and run Flyway for real, so they never touch the shared Neon database (even if your `.env` points at it).
Running the app itself does not need Docker.

## Environment variables
Set them in `backend/.env` (git-ignored, see `.env.example`) or as real environment variables.

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/neuroguard` | JDBC URL |
| `DB_USERNAME` | `neuroguard` | DB user (also used by docker-compose) |
| `DB_PASSWORD` | `neuroguard` | DB password (also used by docker-compose) |
| `DB_NAME`, `DB_PORT` | `neuroguard`, `5432` | docker-compose only |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated allowed origins |
| `JWT_SECRET` | none | HS256 signing key, **at least 32 bytes**. Required outside the `dev` profile (startup fails if missing/short). In `dev`, if unset, a random per-run key is used, so tokens stop working after every restart: set it in `.env` for a stable token. Generate one: `openssl rand -base64 48` |
| `JWT_EXPIRY_MINUTES` | `60` | Access token lifetime |
| `BOOTSTRAP_ADMIN_EMAIL`, `BOOTSTRAP_ADMIN_PASSWORD` | none | If no ADMIN exists at startup, one is created from these (password min 8 chars). Skipped with a warning if unset. The password is never logged. Once an admin exists they are ignored, so remove them from `.env` afterwards. |

All teammates share the same database, so share one `JWT_SECRET` only if you want to reuse each other's tokens (not required).

## API docs
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

## Authentication: how to get a token
1. Make sure an admin exists (set `BOOTSTRAP_ADMIN_EMAIL` / `BOOTSTRAP_ADMIN_PASSWORD` in `.env` for the first start).
2. Log in:
   ```bash
   curl -s -X POST http://localhost:8080/api/auth/staff/login \
     -H 'Content-Type: application/json' \
     -d '{"email":"admin@example.com","password":"your-password"}'
   ```
   Response: `{ "accessToken": "...", "tokenType": "Bearer", "expiresIn": 3600, "userType": "STAFF", "role": "ADMIN", "scope": null }`.
   Patients use `POST /api/auth/patient/login`.
3. Call protected endpoints with `Authorization: Bearer <accessToken>`.
4. In Swagger UI: run the login request under "Authentication", copy `accessToken`, click **Authorize**,
   paste the token (without the word "Bearer"), then try e.g. `GET /api/admin/staff`.

Wrong email and wrong password both return 401 "Invalid email or password". Missing, malformed, expired or
tampered tokens return 401; a valid token without permission returns 403 (same JSON error shape).
Logins (success and failure) and staff changes are written to `audit_log`.
Not implemented yet: login rate limiting/lockout and token revocation (role changes apply after the token expires).

## Endpoints so far
| Endpoint | Access |
|---|---|
| `POST /api/auth/staff/login`, `POST /api/auth/patient/login` | public |
| `POST /api/admin/staff`, `GET /api/admin/staff` (paged: `page`, `size` max 100, `sortBy` = name/email/createdAt), `GET /api/admin/staff/{id}`, `PATCH /api/admin/staff/{id}/role` | Admin |
| `GET /api/me` | Any authenticated user: returns `{ id, name, email, userType, role, scope }` for the caller |
| `GET /api/patients/{patientId}` | Neurologist, Radiologist, MRI tech, Billing clerk, or the patient (own) |

Staff rules: `CLINICAL_STAFF` needs a `permissionScope`, `ADMIN` must have none (400); duplicate email 409;
password 8 to 72 characters; the last remaining ADMIN cannot be demoted (409). Password hashes are never returned.

## Errors
Every error uses one shape:
```json
{ "timestamp": "...", "status": 401, "error": "Unauthorized", "message": "...", "path": "/api/...", "fieldErrors": [ { "field": "...", "message": "..." } ] }
```
Throw `ResourceNotFoundException` (404), `ConflictException` (409) or `BadRequestException` (400) from services.

## Layout
`com.neuroguard.{config, common, security, auth, patient, staff, appointment, clinical, billing, imaging, scanner, audit}`;
each module has `entity/`, `repository/` and placeholder `service/`, `controller/`, `dto/` packages.
Notes: the `service` table maps to `ServiceItem`; `audit_log.actor_id` is polymorphic (staff or patient) so it has no FK.
