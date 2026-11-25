# Clinical records, billing, ICD-10 and dashboard

Backend owner: Nour. Permission rules: [`permissions.md`](permissions.md) (single source of truth).
All endpoints need `Authorization: Bearer <token>`; missing/invalid token -> 401, no permission -> 403,
both in the standard error JSON (see `backend/README.md`, "Errors"). Lists are paged
(`?page=0&size=20`, size max 100) and return `{content, page, size, totalElements, totalPages}`.
Money is always a JSON number with 2 decimals (`1400.00`), computed with `BigDecimal`, scale 2, HALF_UP.

Contents: [Endpoints](#endpoints) · [Business rules](#business-rules) · [ICD-10](#icd-10-reference-data) ·
[Seed data](#seed-data-seed-profile) · [Use of other modules](#use-of-other-modules-note-for-farida)

## Endpoints

| Area | Endpoint | Who |
|---|---|---|
| ICD-10 | `GET /api/icd10?q=`, `GET /api/icd10/{code}` | any staff |
| Diagnoses | `POST /api/patients/{patientId}/diagnoses` | Neurologist |
| | `GET /api/patients/{patientId}/diagnoses`, `GET /api/diagnoses/{id}` | Neurologist, Radiologist, patient (own) |
| Treatment plans | `POST /api/patients/{patientId}/treatment-plans` | Neurologist |
| | `GET /api/patients/{patientId}/treatment-plans` (includes prescriptions) | Neurologist, Radiologist, patient (own) |
| Prescriptions | `POST /api/treatment-plans/{planId}/prescriptions` | Neurologist |
| | `GET /api/treatment-plans/{planId}/prescriptions` | Neurologist, Radiologist, patient (own) |
| Services | `GET /api/services`, `GET /api/services/{id}` | any staff |
| | `POST /api/services`, `PUT /api/services/{id}` | Billing clerk |
| Invoices | `POST /api/invoices`, `POST /api/invoices/{id}/cancel` | Billing clerk |
| | `GET /api/invoices?status=` | Billing clerk, Admin |
| | `GET /api/patients/{patientId}/invoices?status=`, `GET /api/invoices/{id}` | Billing clerk, Admin, patient (own) |
| Payments | `POST /api/invoices/{id}/pay` (no body) | Billing clerk, patient (own) |
| | `GET /api/payments` | Billing clerk, Admin |
| Dashboard *(proposed rules)* | `GET /api/dashboard/{patients/new-per-month, diagnoses/by-category, appointments/by-status, mri-orders/per-week}` | Admin, Neurologist |
| | `GET /api/dashboard/finance` | Admin, Billing clerk |

"Own" for a patient: lists use the `patientId` in the path; single records (`/api/diagnoses/{id}`,
`/api/treatment-plans/{planId}/...`, `/api/invoices/{id}`, `/pay`) are checked in the service. A patient gets
403 for someone else's record **and** for an unknown id; staff get 404 for an unknown id.

### ICD-10
`GET /api/icd10?q=g40.&size=5` (code prefix, dot optional, or part of the description; case-insensitive)
```json
{ "content": [ { "code": "G40", "description": "Epilepsy and recurrent seizures", "category": "G40" } ],
  "page": 0, "size": 5, "totalElements": 1, "totalPages": 1 }
```
`GET /api/icd10/ZZZ99` -> 404.

### Diagnoses
`POST /api/patients/{patientId}/diagnoses` (Neurologist; `diagnosedAt` optional, defaults to now, not in the future)
```json
{ "icd10Code": "g40", "notes": "TEST focal seizures" }
```
201, `Location: /api/diagnoses/{id}`:
```json
{ "id": "…", "patientId": "…", "icd10Code": "G40", "icd10Description": "Epilepsy and recurrent seizures",
  "icd10Category": "G40", "diagnosedAt": "2026-11-05T09:30:00Z", "notes": "TEST focal seizures",
  "staffId": "…", "staffName": "TEST Neurologist", "createdAt": "2026-11-05T09:30:00Z" }
```
Unknown code -> 400 (`fieldErrors[0].field = "icd10Code"`). Lists are newest first (by `diagnosedAt`).

### Treatment plans and prescriptions
`POST /api/patients/{patientId}/treatment-plans`
```json
{ "description": "TEST epilepsy management", "startDate": "2026-09-05", "endDate": "2027-03-05" }
```
`endDate` before `startDate` -> 400 `"endDate must be on or after startDate"`. `endDate` may be omitted.

`POST /api/treatment-plans/{planId}/prescriptions`
```json
{ "medicationDetails": "TEST Levetiracetam 500 mg, twice daily, oral" }
```
`GET /api/patients/{patientId}/treatment-plans` (most recently started first, prescriptions oldest first):
```json
{ "content": [ { "id": "…", "patientId": "…", "description": "TEST epilepsy management",
    "startDate": "2026-09-05", "endDate": "2027-03-05", "staffId": "…", "staffName": "TEST Neurologist",
    "createdAt": "…",
    "prescriptions": [ { "id": "…", "planId": "…", "medicationDetails": "TEST Levetiracetam 500 mg, twice daily, oral",
                         "staffId": "…", "staffName": "TEST Neurologist", "createdAt": "…" } ] } ],
  "page": 0, "size": 20, "totalElements": 1, "totalPages": 1 }
```
The prescriber (`staffId`) is whoever created the prescription, which may differ from the plan's author.

### Service catalogue
`POST /api/services` (Billing clerk) `{ "name": "Brain MRI", "price": 2500 }` -> 201
```json
{ "id": "…", "name": "Brain MRI", "price": 2500.00, "createdAt": "…", "updatedAt": "…" }
```
`PUT /api/services/{id}` takes the same body. Duplicate name (ignoring case and spaces) -> 409; price below 0,
more than 2 decimals or above 99,999,999.99 -> 400.

### Invoices
`POST /api/invoices` (Billing clerk). Only services and quantities: no prices or totals.
```json
{ "patientId": "…", "appointmentId": "…",
  "items": [ { "serviceId": "<Neurology consultation>", "quantity": 1 },
             { "serviceId": "<Brain MRI>", "quantity": 2, "icd10Code": "C71" },
             { "serviceId": "<Report>", "quantity": 2 } ] }
```
201, `Location: /api/invoices/{id}`:
```json
{ "id": "…", "patientId": "…", "appointmentId": "…", "status": "UNPAID", "issueDate": "2026-11-05",
  "totalAmount": 6400.00,
  "items": [
    { "id": "…", "serviceId": "…", "serviceName": "Neurology consultation", "icd10Code": null, "quantity": 1, "unitPrice": 800.00, "lineTotal": 800.00 },
    { "id": "…", "serviceId": "…", "serviceName": "Brain MRI", "icd10Code": "C71", "quantity": 2, "unitPrice": 2500.00, "lineTotal": 5000.00 },
    { "id": "…", "serviceId": "…", "serviceName": "Report", "icd10Code": null, "quantity": 2, "unitPrice": 300.00, "lineTotal": 600.00 } ],
  "payment": null, "createdAt": "…", "updatedAt": "…" }
```
`POST /api/invoices/{id}/cancel` -> 200 with `"status": "CANCELLED"`; on a PAID or CANCELLED invoice -> 409.
`GET /api/invoices?status=UNPAID` filters by `UNPAID`, `PAID` or `CANCELLED` (anything else -> 400).

### Payment (simulated)
`POST /api/invoices/{id}/pay`, **no request body** (any body -> 400). 201:
```json
{ "id": "…", "invoiceId": "…", "patientId": "…", "amountPaid": 6400.00, "status": "COMPLETED",
  "paymentDate": "2026-11-25T10:00:00.123456Z" }
```
The invoice is now `PAID` and its `payment` field shows this object. `GET /api/payments` lists all payments,
newest first.

### Dashboard
All take optional `from`/`to` (`YYYY-MM-DD`, inclusive, UTC; `from` after `to` -> 400).
```
GET /api/dashboard/patients/new-per-month   -> [ { "month": "2026-09", "count": 3 }, ... ]
GET /api/dashboard/diagnoses/by-category    -> [ { "category": "G40", "description": "Epilepsy and recurrent seizures", "count": 3 }, ... ]
GET /api/dashboard/appointments/by-status   -> [ { "status": "SCHEDULED", "count": 3 }, ... all 4 statuses ]
GET /api/dashboard/mri-orders/per-week      -> [ { "isoWeek": "2026-W37", "weekStart": "2026-09-07", "count": 2 }, ... ]
GET /api/dashboard/finance                  -> { "revenue": 10600.00, "completedPayments": 4, "unpaidCount": 2, "unpaidAmount": 9700.00 }
```
Range applies to: patient `created_at`, `diagnosed_at`, appointment `scheduled_time`, MRI order `created_at`,
`payment_date` (revenue) and invoice `issue_date` (unpaid). HL7 and scanner QA metrics are TODO hooks for their
owners (`DashboardService`).

## Business rules

- **Authors come from the token.** The diagnosing/prescribing neurologist and the audit actor are always
  `CurrentUser`; ids in the request body are ignored.
- **Price snapshot.** Each invoice line stores `unit_price`, copied from the catalogue when the invoice is created.
  Changing a service's price later only affects new invoices. (Existing rows were backfilled in V5 from
  `line_total / quantity`, i.e. what was actually charged.)
- **Server-side totals.** `line_total = unit_price × quantity`, `total_amount = Σ line_total`, all `BigDecimal`
  scale 2 HALF_UP. Clients cannot send prices, totals or a status. Every line total and the invoice total must
  be ≤ 99,999,999.99 (the `NUMERIC(10,2)` limit), otherwise 400 and nothing is saved. At least one item;
  quantity 1 to 1000; an ICD-10 code, if given, must exist; the appointment, if given, must belong to the patient.
- **Invoice status machine.**
  ```
  UNPAID --cancel (clerk)-----------------> CANCELLED   (final)
  UNPAID --pay (clerk or own patient)-----> PAID        (final)
  ```
  Cancel or pay on a final invoice -> 409. Both lock the invoice row, so a concurrent cancel and payment (or a
  double click on "pay") cannot both succeed.
- **Simulated payment.** No payment provider and no card, account or bank data anywhere (no such columns or
  fields; the pay endpoint rejects any body). Paying creates one `COMPLETED` payment for the full total and sets
  the invoice to `PAID` in the same transaction as its audit entry. An invoice totalling 0.00 cannot be paid (409,
  because `amount_paid` must be > 0); cancel it instead.
- **Revenue** on the dashboard is the sum of `COMPLETED` payments only; unpaid invoices are reported separately and
  cancelled invoices appear in neither.
- **Audit.** `DIAGNOSIS_CREATED`, `TREATMENT_PLAN_CREATED`, `PRESCRIPTION_CREATED`, `SERVICE_CREATED`,
  `SERVICE_UPDATED`, `INVOICE_CREATED`, `INVOICE_CANCELLED`, `PAYMENT_COMPLETED`, each written in the same
  transaction as the change (no change without its audit row, and vice versa).
- **Migrations.** V3 (ICD-10 table + FKs), V4 (`diagnosis.notes`, `prescription.staff_id`), V5
  (`invoice_item.unit_price`, unique `service.name`).

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
