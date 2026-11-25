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
