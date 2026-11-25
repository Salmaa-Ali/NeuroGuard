# Clinical records, billing, ICD-10 and dashboard

Backend owner: Nour. Permission rules live in [`permissions.md`](permissions.md).
This file grows step by step; the full endpoint reference is added at the end of the work.

## ICD-10 reference data

Table `icd10_code` (migration V3): `code` (PK, upper case, no dot, e.g. `G40909`), `description`,
`category` (first 3 characters, used for grouping). `diagnosis.icd10_code` and `invoice_item.icd10_code`
have foreign keys to it.

- **Starter set** (in V3): G40, G35, G43, I63, C71, D32, G20, G30 with their ICD-10-CM category titles.
- **Source for the full list:** the CMS ICD-10-CM "order file" (`icd10cm_order_YYYY.txt`, public domain),
  inside the yearly "Code Descriptions in Tabular Order" zip on the CMS ICD-10 page
  (https://www.cms.gov/medicare/coding-billing/icd-10-codes). About 74,000 lines, headers and billable codes.
  **Do not commit the data file** (it is git-ignored).
- **API:** `GET /api/icd10?q=` (code prefix or description, case-insensitive, dot optional) and
  `GET /api/icd10/{code}`; any staff, patients 403.

### Importing the full list (`icd10-import` profile)

The importer reads the fixed-width order file (columns 7-13 code, 78-end long description) and upserts:
new codes are inserted, codes whose description changed are updated, identical ones are left alone, and
codes missing from the file are never deleted (diagnoses and invoices may reference them). It is
idempotent: a second run reports 0 inserted, 0 updated. Everything runs in one transaction. At the end it
logs one summary line (the numbers depend on the yearly file):

```
ICD-10 import finished: ImportResult[linesRead=<n>, inserted=<n>, updated=<n>, unchanged=<n>, skipped=<n>]
```

`skipped` counts malformed lines and repeated codes (each is also logged as a warning).

**Before running: check which database `backend/.env` points at.** The import writes to that database.
Never run it from a feature branch against the shared Neon DB (Flyway would also apply unmerged migrations).

```powershell
cd backend
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev,icd10-import" `
  "-Dspring-boot.run.arguments=--icd10.import.file=C:/data/icd10cm_order_2026.txt --spring.main.web-application-type=none"
```

- Keep `dev` in the profile list (otherwise `JWT_SECRET` becomes mandatory).
- `--spring.main.web-application-type=none` makes the app exit after the import instead of starting the server.
- A missing or unreadable file, or no `icd10.import.file`, stops startup with a clear error.

## Seed data (`seed` profile)

Fictional demo data with **fixed 2026 dates**, so the dashboard totals below never change. Every person
record is named `TEST ...`. The seed is idempotent: if `test.admin@neuroguard.local` or patient `TEST-P01`
already exists, it does nothing. It runs in one transaction (all or nothing).

**Safety guard:** the seed refuses to start if the database URL in use contains `neon.tech` (the shared team
DB), unless `seed.allow-shared-db=true` is set on purpose. Only the host is logged, never credentials.

```powershell
cd backend
# 1. Check backend/.env first: DB_URL must point at a local or throwaway database, not Neon.
# 2. Seed and keep the app running (Swagger at http://localhost:8080/swagger-ui.html):
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev,seed"
# or seed and exit:
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev,seed" "-Dspring-boot.run.arguments=--spring.main.web-application-type=none"
```

### Logins (dev only)
Every seeded account uses the password **`NeuroGuard-Dev-2026!`**. It is public in this repository, so it
must never be used on a real system.

| Account | Email | Login endpoint |
|---|---|---|
| TEST Admin | `test.admin@neuroguard.local` | `POST /api/auth/staff/login` |
| TEST Neurologist | `test.neurologist@neuroguard.local` | staff |
| TEST Radiologist | `test.radiologist@neuroguard.local` | staff |
| TEST MRI Technologist | `test.mritech@neuroguard.local` | staff |
| TEST Biomedical Engineer | `test.biomed@neuroguard.local` | staff |
| TEST Billing Clerk | `test.billing@neuroguard.local` | staff |
| TEST Patient 01 ... 10 | `test.patient01@neuroguard.local` ... `test.patient10@neuroguard.local` | `POST /api/auth/patient/login` |

### What is created
| Data | Count | Details |
|---|---|---|
| Staff | 6 | admin + one per scope |
| Patients | 10 | `TEST-P01` ... `TEST-P10`, created Sep (3), Oct (3), Nov (4) |
| Services | 4 | Neurology consultation 800.00, Brain MRI 2500.00, MRI with contrast 3500.00, Report 300.00 (EGP). An existing service with the same name is reused, with its own price. |
| Appointments | 12 | SCHEDULED 3, COMPLETED 6, CANCELLED 2, MISSED 1 |
| MRI orders | 8 | across 5 ISO weeks |
| Diagnoses | 9 | starter codes G40, G43, G35, I63, C71, G20 |
| Treatment plans / prescriptions | 4 / 7 | prescribed by TEST Neurologist |
| Invoices | 7 | PAID 4, UNPAID 2, CANCELLED 1 (table below) |
| Payments | 4 | COMPLETED, one per PAID invoice |

| Invoice | Patient | Issued | Lines | Total | Status | Paid on |
|---|---|---|---|---|---|---|
| 1 | P01 | 2026-09-05 | consultation 1, Brain MRI 1, report 1 | 3600.00 | PAID | 2026-09-06 |
| 2 | P02 | 2026-09-18 | consultation 1 | 800.00 | PAID | 2026-09-18 |
| 3 | P03 | 2026-10-01 | consultation 1, MRI with contrast 1, report 1 | 4600.00 | PAID | 2026-10-03 |
| 4 | P04 | 2026-10-08 | consultation 1, Brain MRI 1 | 3300.00 | UNPAID | |
| 5 | P05 | 2026-10-22 | Brain MRI 1 | 2500.00 | CANCELLED | |
| 6 | P07 | 2026-11-05 | consultation 1, Brain MRI 2, report 2 | 6400.00 | UNPAID | |
| 7 | P08 | 2026-11-12 | consultation 2 | 1600.00 | PAID | 2026-11-13 |

### Expected dashboard totals (on an otherwise empty database)
Checked automatically by `SeedTest`; if you change the seed, update this table and the test together.

| Endpoint | Expected |
|---|---|
| `/api/dashboard/patients/new-per-month` | 2026-09: 3, 2026-10: 3, 2026-11: 4 |
| `/api/dashboard/diagnoses/by-category` | G40: 3, G43: 2, C71: 1, G20: 1, G35: 1, I63: 1 |
| `/api/dashboard/appointments/by-status` | SCHEDULED 3, COMPLETED 6, CANCELLED 2, MISSED 1 |
| `/api/dashboard/mri-orders/per-week` | 2026-W37: 2, W39: 1, W41: 2, W43: 1, W45: 2 |
| `/api/dashboard/finance` | revenue **10600.00** (4 payments), unpaid **2** invoices = **9700.00** |
| `/api/dashboard/finance?from=2026-11-01&to=2026-11-30` | revenue 1600.00 (1 payment), unpaid 1 = 6400.00 |

## Use of other modules (note for Farida)
Nothing in the auth/security, patient, staff, appointment, imaging, audit or scanner modules was changed,
except adding constants to `security/Authz.java` (permission expressions) and `audit/AuditAction.java`
(action names), as agreed. Two new helpers were added to the shared `common` package (no existing file changed):
`PatientRecordAccess` (the "own record" check for records loaded by their own id) and `Pages` (paging limits).
Read-only use:
- `PatientRepository`, `StaffRepository`: look up the patient and the acting staff member.
- `AppointmentRepository`: check that an invoice's appointment belongs to the same patient.
- `AuditService.record(...)`: audit every clinical and billing write.
- Dashboard: aggregate `SELECT`s on `patient`, `appointment` and `mri_order` (no writes).
- Seed (`seed` profile only): inserts TEST rows into `staff`, `patient`, `appointment` and `mri_order` with
  plain SQL, without using or changing their services.
