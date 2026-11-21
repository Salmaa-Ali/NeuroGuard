import { http, HttpResponse } from 'msw'
import { setupServer } from 'msw/node'

export const API = 'http://localhost:8080'

/** The profile endpoint every logged-in test needs. Override per test with server.use(...). */
export const defaultMe = {
  id: '00000000-0000-4000-8000-000000000001',
  name: 'Test User',
  email: 'test.user@example.com',
  userType: 'STAFF',
  role: 'ADMIN',
  scope: null,
}

export const server = setupServer(http.get(`${API}/api/me`, () => HttpResponse.json(defaultMe)))

/** A backend error in the standard JSON shape. */
export function apiError(
  status: number,
  message: string,
  fieldErrors: { field: string; message: string }[] = [],
) {
  return HttpResponse.json(
    {
      timestamp: new Date().toISOString(),
      status,
      error: 'Error',
      message,
      path: '/test',
      fieldErrors,
    },
    { status },
  )
}
