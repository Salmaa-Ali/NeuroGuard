import axios from 'axios'
import { clearSession, getSession } from '../auth/session'

export const API_BASE_URL: string =
  (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080'

export const api = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
})

/** Login requests legitimately return 401 (wrong credentials); that is not an expired session. */
const isAuthEndpoint = (url?: string) => !!url && url.startsWith('/api/auth/')

let onSessionExpired: (() => void) | null = null

/** Lets the app react (e.g. clear cached data) when the server rejects the token. */
export function setSessionExpiredHandler(handler: (() => void) | null) {
  onSessionExpired = handler
}

api.interceptors.request.use((config) => {
  const session = getSession()
  if (session && !isAuthEndpoint(config.url)) {
    config.headers.set('Authorization', `Bearer ${session.token}`)
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      !isAuthEndpoint(error.config?.url) &&
      getSession()
    ) {
      // 401 -> the route guard sends the user to /login, which shows "Session expired".
      clearSession('expired')
      onSessionExpired?.()
    }
    // 403 is left to the page: it shows the "no access" screen and the user stays logged in.
    return Promise.reject(error)
  },
)
