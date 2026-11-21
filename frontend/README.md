# NeuroGuard Frontend

React + Vite + TypeScript client for the NeuroGuard backend (`../backend`). Status: app shell, login,
role-based navigation, staff management and the patient summary page work against the real backend;
everything else is a placeholder for the next phases.

Stack: React 19, React Router, TanStack Query, axios, Tailwind CSS v4, react-hook-form + zod,
Vitest + React Testing Library + msw. Types are generated from the backend's OpenAPI document.

## Setup

```bash
cd frontend
cp .env.example .env.local     # optional: only needed to change the backend URL
npm install
npm run dev                    # http://localhost:5173
```

The backend must be running (see `../backend/README.md`, default `http://localhost:8080`). It only allows
CORS from `http://localhost:5173`, so the dev server uses that exact port and fails if it is taken
(`strictPort`). Needs Node 20+.

## Environment variables

| Variable            | Default                 | Purpose                             |
| ------------------- | ----------------------- | ----------------------------------- |
| `VITE_API_BASE_URL` | `http://localhost:8080` | Backend base URL, no trailing slash |

Only variables prefixed with `VITE_` reach the browser. Never put secrets here.

## Scripts

| Script                            | What it does                                                                                        |
| --------------------------------- | --------------------------------------------------------------------------------------------------- |
| `npm run dev`                     | Dev server on :5173                                                                                 |
| `npm run build`                   | Type-check and production build into `dist/`                                                        |
| `npm run preview`                 | Serve the production build locally                                                                  |
| `npm run typecheck`               | TypeScript only                                                                                     |
| `npm run lint`                    | ESLint                                                                                              |
| `npm run format` / `format:check` | Prettier (write / check)                                                                            |
| `npm test` / `npm run test:watch` | Vitest                                                                                              |
| `npm run gen:api`                 | Regenerate `src/api/schema.d.ts` from `http://localhost:8080/v3/api-docs` (backend must be running) |

Run `npm run gen:api` whenever the backend API changes and commit the result. The domain types in
`src/api/types.ts` are derived from the generated file, so a breaking change shows up as a compile error.

## How login and permissions work

- One login form for everybody: the app calls `POST /api/auth/staff/login` and, only if that answers 401, falls
  back to `/api/auth/patient/login` (any other error stops immediately). The user's id is the `sub`
  claim of the JWT; the name comes from `GET /api/me`, which the app calls after login and on every refresh.
- The session lives in memory and `sessionStorage` (a refresh keeps you logged in, closing the tab logs you out).
  The app logs out by itself when the token expires.
- An axios interceptor adds `Authorization: Bearer <token>`.
  **401** (not from the login call) clears the session and returns to `/login` with "Session expired".
  **403** shows the "You don't have access to this page" screen and keeps you logged in.
- Backend errors share one shape (`{ timestamp, status, error, message, path, fieldErrors }`): the `message` is
  shown and `fieldErrors` are put on the matching form fields (`src/api/errors.ts`).
- **Who sees what** is one table: `src/auth/access.ts` (mirrors `../docs/permissions.md`). A user is allowed if their
  role is in `roles` OR their permission scope is in `scopes`. Hiding menu items and pages is a convenience only;
  the backend enforces the real rules. `src/layout/nav.test.ts` locks the menu per role.

## Folder structure

```
src/
  api/        axios client + interceptors, error helpers, generated schema.d.ts, domain types
  auth/       AuthProvider/useAuth, session store, token helpers, access rules, RequireAuth, RequireRole
  components/ Button, Input, Select, Table, Dialog, Toast, PageHeader, EmptyState, ErrorState, ...
  layout/     AppShell, Sidebar, Topbar, nav.ts (menu built from the features)
  features/   one folder per module, each with feature.tsx (routes + menu) and api.ts
    admin-staff  auth  dashboard  errors  patients            (working)
    appointments  billing  clinical  imaging (+ viewer)  reports  scanners  audit  cdss   (placeholders)
  routes.tsx  collects every feature's routes; wraps each in <RequireRole>
  test/       msw server, render helpers, fake tokens
```

## Add a new feature page

Teammates should not need to touch shared files.

1. Create the page in your feature's folder, e.g. `src/features/appointments/AppointmentsPage.tsx`
   (replace the `PlaceholderPage`). Keep pages thin: call hooks, render components.
2. Put server calls and TanStack Query hooks in the feature's `api.ts` (use `api` from `src/api/client.ts`).
3. Register the route in the feature's `feature.tsx`:
   ```tsx
   { path: '/appointments', element: <AppointmentsPage />, access: ACCESS.appointments, navLabel: 'Appointments' }
   ```
   `access` is required (it feeds `<RequireRole>`); `navLabel` puts it in the sidebar for allowed users only.
   If the page needs a new rule, add it to `ACCESS` in `src/auth/access.ts`, and to `docs/permissions.md`.
4. A brand new feature: create `src/features/<name>/feature.tsx` exporting a `Feature` and add it to the list in
   `src/features/index.ts` (the order is the sidebar order).
5. Show loading, empty and error states (`LoadingState`, `EmptyState`, `ErrorState`; `ErrorState` already turns a
   403 into the no-access screen). Forms: react-hook-form + zod, and `applyFieldErrors` for server errors.
6. Add tests next to the code (`*.test.tsx`). `renderApp(route)` renders the real routes; `loginAs({ role, scope })`
   puts a session in place; `server.use(http.get(...))` mocks the backend. Unmocked requests fail the test.

The DICOM viewer route is `/studies/:studyUid/viewer` (`src/features/imaging/viewer/ViewerPage.tsx`): an empty
container for Cornerstone3D in Phase 3.

## Differences from the backend contract

The live `/v3/api-docs` matches the brief, with these notes:

- Error responses (and the `ApiError` shape) are not in the OpenAPI document, so `ApiError` is hand-written in `src/api/errors.ts`.
- Response fields are not marked required, so the generator makes them optional; `src/api/types.ts` makes them
  required again and marks the really nullable ones (`scope`, `permissionScope`, `phone`, `neurologyHistory`).
- The document lists `POST /api/admin/staff` as 200; it really returns 201.
- Sorting is ascending only (`sortBy`, no direction parameter).
- `GET /api/me` was added to the backend for this client (name, email, role, scope of the caller).
- The login response and the token carry no name, hence `/api/me`.

## Try it by hand

1. Start the backend and `npm run dev`; log in as the admin.
2. Staff management > Add staff: create a Neurologist and a Billing clerk (scope dropdown appears for clinical staff).
3. Change a role from the table; log in as each user (another browser profile or after logging out) and watch the menu change.
4. Log in as a patient: My profile loads; opening `/admin/staff` shows the 403 screen.
5. Expiry: set `JWT_EXPIRY_MINUTES=1` in the backend `.env`, log in and wait a minute.
