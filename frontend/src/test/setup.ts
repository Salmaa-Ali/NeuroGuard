import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterAll, afterEach, beforeAll } from 'vitest'
import { api } from '../api/client'
import { clearSession } from '../auth/session'
import { server } from './server'

// jsdom's XHR enforces CORS against the mocked API (a different origin), which a real browser
// would not block for our backend. The fetch adapter has no such check.
api.defaults.adapter = 'fetch'

// Any request without a handler fails the test, so nothing silently hits a real backend.
beforeAll(() => server.listen({ onUnhandledFrame: 'error' }))

afterEach(() => {
  cleanup()
  server.resetHandlers()
  clearSession()
  sessionStorage.clear()
})

afterAll(() => server.close())
