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
| `SCANNER_QA_DUE_SOON_DAYS` | `30` | `app.scanner.qa.due-soon-days`: a scanner whose QA is due within this many days shows as `DUE_SOON` |
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

## Scanner QA (calibration) module
Biomedical engineers record QA sessions for MRI scanners; everyone else in `Authz.SCANNER_READ` can read them.
Endpoints and permissions: see [`docs/permissions.md`](../docs/permissions.md). Migration: `V7__scanner_qa.sql`.

**Safety:** a failed or overdue QA status means **"Flagged for review"**. It never blocks, invalidates or
changes any patient study or report. The risk score is **indicative**, not a safety verdict.

- **Tolerances are data**, entered per QA test by the biomedical engineer (`qa_test.lower_limit` /
  `upper_limit`, either may be empty = no bound on that side). They are never hard-coded.
- **Recording a session** (`POST /api/scanners/{id}/calibrations`): the client sends only `performedAt`,
  notes, certificate ref and `{ qaTestId, measuredValue }` rows. The server copies each test's current limits
  onto the measurement (so later tolerance edits never rewrite history), checks every value
  (inclusive bounds), sets `result` = PASS only if all are within tolerance, `next_due_date` = performed date
  (UTC) + the scanner's `calibration_interval_days`, `performed_by` = the caller, and stores the risk score.
  Errors: unknown/inactive test or the same test twice 400, `performedAt` in the future 400,
  RETIRED scanner 409, duplicate serial number 409.
- **QA status** (derived on every read, never stored), from the latest session by `performed_at`:
  `NEVER_CALIBRATED` (no session), `FAILED` (latest result FAIL), `OVERDUE` (today after the due date),
  `DUE_SOON` (due within `app.scanner.qa.due-soon-days`, default 30, inclusive), else `OK`.
  "Today" is the UTC date from the injectable `Clock` bean (tests use a fixed `MutableClock`).
  `qa-overview` counts leave out RETIRED scanners.
- **Risk score (indicative), 0 to 100** (`RiskScoreCalculator`):

  `risk = round1( 100 × (0.5·F + 0.3·D + 0.2·O) )`, clamped to 0..100
  - **F** failures: failed measurements ÷ all measurements in the last 3 sessions (the new one included).
  - **D** drift: for passing measurements with both limits, `1 − margin ÷ (range/2)` where
    `margin = min(value − lower, upper − value)`; 0 at the centre of the band, 1 on a limit; averaged.
    One-sided or unbounded tests are left out.
  - **O** overdue: days the new session was performed after the previous due date ÷ the interval, capped at 1.

  The weights are a documented heuristic, not a standard. The score is stored on the session when recorded.
- **`GET /api/scanners/{id}/qa-summary`** is a compact read-only object for the Phase 3 viewer's equipment
  panel (frontend: `<ScannerQaPanel scannerId=… />`), with `flaggedForReview` and the ids/timestamps it comes
  from (`sourceSessionId`, `sourceSessionRecordedAt`, `evaluatedOn`).
- **Demo data (dev profile only):** `ScannerDemoSeeder` adds two `TEST` scanners with coils, the seven ACR
  large-phantom QA tests and a session history (one scanner `OK`, the other `OVERDUE`, with an earlier FAIL).
  It runs once (skipped if a `TEST` scanner exists) and never in tests. Because the default profile is `dev`
  and the team DB is Neon, the first start after this lands seeds the shared DB. Seeded sessions are
  attributed to an existing biomedical engineer, or to a `TEST` engineer account that cannot log in.
  **The seeded ACR tolerances are example placeholder values** (`is_placeholder = true`, shown as
  "Placeholder: confirm with medical physicist"), used until the team decides on real tolerances.
  They are not for clinical use.

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
