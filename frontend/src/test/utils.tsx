import { QueryClient } from '@tanstack/react-query'
import { render } from '@testing-library/react'
import type { ReactElement } from 'react'
import { createMemoryRouter, MemoryRouter, RouterProvider } from 'react-router-dom'
import { AppProviders } from '../AppProviders'
import type { LoginResult, PermissionScope, Role, UserType } from '../api/types'
import { setSession } from '../auth/session'
import { appRoutes } from '../routes'

const b64url = (value: unknown) =>
  btoa(JSON.stringify(value)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')

/** An unsigned fake JWT: the client only ever decodes it for display. */
export function makeJwt(claims: Record<string, unknown>) {
  return `${b64url({ alg: 'HS256', typ: 'JWT' })}.${b64url(claims)}.signature`
}

export const TEST_USER_ID = '11111111-1111-4111-8111-111111111111'

export interface TestUser {
  role: Role
  scope?: PermissionScope | null
  id?: string
  email?: string
  /** Milliseconds until the token expires (default one hour). */
  ttlMs?: number
}

/** Puts a logged-in session in place before rendering. */
export function loginAs({
  role,
  scope = null,
  id = TEST_USER_ID,
  email = 'me@example.com',
  ttlMs = 3_600_000,
}: TestUser) {
  const userType: UserType = role === 'PATIENT' ? 'PATIENT' : 'STAFF'
  const expiresAt = Date.now() + ttlMs
  setSession({
    token: makeJwt({ sub: id, exp: Math.floor(expiresAt / 1000), role, user_type: userType }),
    expiresAt,
    userId: id,
    email,
    userType,
    role,
    scope,
  })
}

/** A fake successful login response (the token's "sub" is the user id). */
export function loginResponse(
  role: Role,
  scope: PermissionScope | null = null,
  id = TEST_USER_ID,
): LoginResult {
  const exp = Math.floor(Date.now() / 1000) + 3600
  const userType: UserType = role === 'PATIENT' ? 'PATIENT' : 'STAFF'
  return {
    accessToken: makeJwt({
      sub: id,
      exp,
      role,
      user_type: userType,
      perm_scope: scope ?? undefined,
    }),
    tokenType: 'Bearer',
    expiresIn: 3600,
    userType,
    role,
    scope,
  }
}

function newClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  })
}

/** Renders a piece of UI with auth, query and toast providers inside a MemoryRouter. */
export function renderWithProviders(ui: ReactElement, { route = '/' }: { route?: string } = {}) {
  return render(
    <AppProviders client={newClient()}>
      <MemoryRouter initialEntries={[route]}>{ui}</MemoryRouter>
    </AppProviders>,
  )
}

/** Renders the whole app (real routes, guards, shell) at a URL. */
export function renderApp(route = '/') {
  const router = createMemoryRouter(appRoutes, { initialEntries: [route] })
  return {
    router,
    ...render(
      <AppProviders client={newClient()}>
        <RouterProvider router={router} />
      </AppProviders>,
    ),
  }
}
