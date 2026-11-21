/**
 * The login session: kept in memory and mirrored to sessionStorage, so a refresh keeps you logged in
 * but closing the tab logs you out. A tiny external store so the axios client (outside React) and
 * React (useSyncExternalStore) share one source of truth.
 */
import type { LoginResult, PermissionScope, Role, UserType } from '../api/types'
import { getExpiryMs, getUserId } from './token'

export interface Session {
  token: string
  /** Epoch milliseconds. */
  expiresAt: number
  userId: string
  /** Email typed at login; shown until /api/me returns the real profile. */
  email: string
  userType: UserType
  role: Role
  scope: PermissionScope | null
}

export type LogoutReason = 'expired'

const STORAGE_KEY = 'neuroguard.session'

function isSession(value: unknown): value is Session {
  const s = value as Partial<Session> | null
  return (
    !!s &&
    typeof s.token === 'string' &&
    typeof s.expiresAt === 'number' &&
    typeof s.userId === 'string' &&
    typeof s.email === 'string' &&
    typeof s.userType === 'string' &&
    typeof s.role === 'string'
  )
}

function readStorage(): Session | null {
  try {
    const raw = sessionStorage.getItem(STORAGE_KEY)
    if (!raw) return null
    const parsed: unknown = JSON.parse(raw)
    if (!isSession(parsed) || parsed.expiresAt <= Date.now()) {
      sessionStorage.removeItem(STORAGE_KEY)
      return null
    }
    return parsed
  } catch {
    return null
  }
}

let current: Session | null = readStorage()
let logoutReason: LogoutReason | null = null
const listeners = new Set<() => void>()

const emit = () => listeners.forEach((l) => l())

export const getSession = () => current
export const getLogoutReason = () => logoutReason

export function subscribe(listener: () => void) {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function setSession(session: Session) {
  current = session
  logoutReason = null
  try {
    sessionStorage.setItem(STORAGE_KEY, JSON.stringify(session))
  } catch {
    /* storage unavailable: the in-memory session still works */
  }
  emit()
}

export function clearSession(reason?: LogoutReason) {
  current = null
  logoutReason = reason ?? null
  try {
    sessionStorage.removeItem(STORAGE_KEY)
  } catch {
    /* ignore */
  }
  emit()
}

/** Builds a session from a login response. The user id comes from the token's "sub" claim. */
export function createSession(login: LoginResult, email: string): Session {
  const userId = getUserId(login.accessToken)
  if (!userId) throw new Error('The server returned an invalid token')
  return {
    token: login.accessToken,
    expiresAt: getExpiryMs(login.accessToken) ?? Date.now() + login.expiresIn * 1000,
    userId,
    email: email.trim(),
    userType: login.userType,
    role: login.role,
    scope: login.scope,
  }
}
