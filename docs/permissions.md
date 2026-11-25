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
| Dashboard: clinical sections (aggregates) *(proposed, confirm with team)* | R | R | - | - | - | - | - |
| Dashboard: finance section *(proposed, confirm with team)* | R | - | - | - | - | R | - |
| ICD-10 codes (reference data) | R | R | R | R | R | R | - |
| Service catalogue (billable services) | R | R | R | R | R | RW | - |

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
  Use `common.PatientRecordAccess.requireReadable(...)`: patients get 403 for someone else's record **and** for
  an unknown id (no probing which ids exist); staff get 404 for an unknown id.
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
| `GET /api/icd10?q=`, `GET /api/icd10/{code}` | Any staff (`Authz.ANY_STAFF`); patients 403 |
| `POST /api/patients/{patientId}/diagnoses` | Neurologist (`Authz.CLINICAL_WRITE`) |
| `GET /api/patients/{patientId}/diagnoses` | Neurologist, Radiologist, or the patient (own) (`Authz.CLINICAL_READ_BY_PATIENT`) |
| `GET /api/diagnoses/{id}` | Neurologist, Radiologist, or the patient if the diagnosis is theirs (`Authz.CLINICAL_READ_BY_RECORD` + service check) |
| `POST /api/patients/{patientId}/treatment-plans` | Neurologist (`Authz.CLINICAL_WRITE`) |
| `GET /api/patients/{patientId}/treatment-plans` (plans include their prescriptions) | Neurologist, Radiologist, or the patient (own) (`Authz.CLINICAL_READ_BY_PATIENT`) |
| `POST /api/treatment-plans/{planId}/prescriptions` | Neurologist (`Authz.CLINICAL_WRITE`) |
| `GET /api/treatment-plans/{planId}/prescriptions` | Neurologist, Radiologist, or the patient if the plan is theirs (`Authz.CLINICAL_READ_BY_RECORD` + service check) |
| `GET /api/services`, `GET /api/services/{id}` | Any staff (`Authz.ANY_STAFF`); patients 403 |
| `POST /api/services`, `PUT /api/services/{id}` | Billing clerk (`Authz.BILLING_WRITE`) |
| `POST /api/invoices`, `POST /api/invoices/{id}/cancel` | Billing clerk (`Authz.BILLING_WRITE`) |
| `GET /api/invoices?status=` | Billing clerk or Admin (`Authz.INVOICE_READ_ALL`) |
| `GET /api/patients/{patientId}/invoices?status=` | Billing clerk, Admin, or the patient (own) (`Authz.INVOICE_READ_BY_PATIENT`) |
| `GET /api/invoices/{id}` | Billing clerk, Admin, or the patient if the invoice is theirs (`Authz.INVOICE_READ_BY_RECORD` + service check) |
| `POST /api/invoices/{id}/pay` (simulated, no request body) | Billing clerk, or the patient if the invoice is theirs (`Authz.INVOICE_PAY` + service check) |
| `GET /api/payments` | Billing clerk or Admin (`Authz.INVOICE_READ_ALL`) |
| `GET /api/dashboard/patients/new-per-month`, `/diagnoses/by-category`, `/appointments/by-status`, `/mri-orders/per-week` | Admin or Neurologist (`Authz.DASHBOARD_CLINICAL`) **proposed, confirm with team** |
| `GET /api/dashboard/finance` | Admin or Billing clerk (`Authz.DASHBOARD_FINANCE`) **proposed, confirm with team** |

## Proposed rules to confirm
- **Dashboard** (`/api/dashboard/*`): Admin sees every section, the Billing clerk only finance, the Neurologist
  only the clinical sections; everyone else 403. The clinical sections are counts only (no patient ids or names),
  which is why Admin may see "diagnoses by ICD-10 category" although Admin has no access to individual diagnoses.
  Please confirm or change before the frontend relies on it.

## Known limits (for later)
- Role changes do not revoke tokens already issued; they carry the old role until they expire.
- No login rate limiting or lockout yet.
