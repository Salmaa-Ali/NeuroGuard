# NeuroGuard permission matrix

Single source of truth for who can do what. Controllers must follow this table; if you need to change
a rule, change it here in the same PR as the code.

`R` = read, `W` = write, `-` = no access, **own** = only records with the patient's own `patient_id`.

| Resource | Admin | Neurologist | Radiologist | MRI tech | Biomed eng | Billing clerk | Patient |
|---|---|---|---|---|---|---|---|
| Staff accounts | RW | - | - | - | - | - | - |
| Patient profile & history | - | RW | R | R | - | R | own RW |
| Appointments | R | RW | R | R | - | R | own R |
| MRI orders | - | RW | R | R + status update | - | - | own R |
| Diagnoses, treatment plans, prescriptions | - | RW | R | - | - | - | own R |
| Invoices & payments | R | - | - | - | - | RW | own R + pay |
| Scanners & calibration | R | R | R | R | RW | - | - |
| Audit log | R | - | - | - | - | - | - |

## Identity model
- Two account types: **staff** (table `staff`) and **patients** (table `patient`).
- Staff have a `base_role`: `ADMIN` or `CLINICAL_STAFF`. Clinical staff also have a `permission_scope`:
  `NEUROLOGIST`, `RADIOLOGIST`, `MRI_TECHNOLOGIST`, `BIOMEDICAL_ENGINEER`, `BILLING_CLERK`.
  Admins have no scope.
- Admin is not a clinical role: it has no access to patient records, diagnoses or MRI orders.

## JWT claims (HS256, issuer `neuroguard`)
| Claim | Values |
|---|---|
| `sub` | user UUID |
| `user_type` | `STAFF` or `PATIENT` |
| `role` | `ADMIN`, `CLINICAL_STAFF`, `PATIENT` |
| `perm_scope` | the five scopes above; only for `CLINICAL_STAFF` |
| `iat`, `exp`, `iss` | issued-at, expiry (default 60 min), `neuroguard` |

The claim is deliberately not called `scope` (Spring maps that one automatically).
Tokens with inconsistent claims (e.g. `CLINICAL_STAFF` without `perm_scope`) are rejected with 401.

## Authorities (what `@PreAuthorize` sees)
- `role` -> `ROLE_ADMIN` / `ROLE_CLINICAL_STAFF` / `ROLE_PATIENT`
- `perm_scope` -> `PERM_NEUROLOGIST`, `PERM_RADIOLOGIST`, `PERM_MRI_TECHNOLOGIST`,
  `PERM_BIOMEDICAL_ENGINEER`, `PERM_BILLING_CLERK`

## How to apply it in code
- Put shared expressions in `com.neuroguard.security.Authz` (constants), not inline in controllers.
- Use the `@access` bean (`AccessChecks`): `@access.hasAnyScope('NEUROLOGIST', ...)` and `@access.isSelf(#patientId)`.
- Read the caller with `CurrentUser.require()` (id, userType, role, scope). Never parse the token in a service.
- For "own" rules, the path/query parameter must be named `patientId` (or pass the id explicitly to `isSelf`),
  and services that load a record by another id (e.g. an invoice) must compare its patient to `CurrentUser`.
- Example:
  ```java
  @PreAuthorize("(hasRole('CLINICAL_STAFF') and @access.hasAnyScope('NEUROLOGIST','RADIOLOGIST')) or @access.isSelf(#patientId)")
  ```
- Status codes: no/invalid/expired token -> 401, valid token without permission -> 403 (both in the standard
  error JSON). `@PreAuthorize` runs before the service, so patients get 403 (not 404) for ids that are not theirs.

## Implemented so far
| Endpoint | Rule |
|---|---|
| `POST /api/auth/staff/login`, `POST /api/auth/patient/login` | public |
| `GET /api/me` | Any authenticated user (own identity) |
| `POST/GET /api/admin/staff`, `GET /api/admin/staff/{id}`, `PATCH /api/admin/staff/{id}/role` | Admin only |
| `GET /api/patients/{patientId}` | Neurologist, Radiologist, MRI tech, Billing clerk, or the patient (own) |

## Known limits (for later)
- Role changes do not revoke tokens already issued; they carry the old role until they expire.
- No login rate limiting or lockout yet.
