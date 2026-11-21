import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useSyncExternalStore, type ReactNode } from 'react'
import type { LoginInput } from '../api/types'
import { fetchMe, loginRequest, type LoginKind } from './api'
import { AuthContext, type AuthContextValue, type AuthUser } from './context'
import { clearSession, createSession, getSession, setSession, subscribe } from './session'

/** setTimeout cannot wait longer than a signed 32-bit number of milliseconds. */
const MAX_TIMEOUT_MS = 2 ** 31 - 1

export function AuthProvider({ children }: { children: ReactNode }) {
  const session = useSyncExternalStore(subscribe, getSession)
  const queryClient = useQueryClient()

  // Who am I? (name for the top bar). The token itself only carries id, role and scope.
  const me = useQuery({
    queryKey: ['me', session?.userId],
    queryFn: fetchMe,
    enabled: session !== null,
    staleTime: Infinity,
    retry: false,
  })

  // Log out automatically when the token expires.
  useEffect(() => {
    if (!session) return
    const expire = () => {
      clearSession('expired')
      queryClient.clear()
    }
    const remaining = session.expiresAt - Date.now()
    if (remaining <= 0) {
      expire()
      return
    }
    const timer = setTimeout(expire, Math.min(remaining, MAX_TIMEOUT_MS))
    return () => clearTimeout(timer)
  }, [session, queryClient])

  const login = useCallback(async (kind: LoginKind, credentials: LoginInput) => {
    const result = await loginRequest(kind, credentials)
    setSession(createSession(result, credentials.email))
  }, [])

  const logout = useCallback(() => {
    clearSession()
    queryClient.clear()
  }, [queryClient])

  const user = useMemo<AuthUser | null>(
    () =>
      session && {
        id: session.userId,
        name: me.data?.name ?? null,
        email: me.data?.email ?? session.email,
        userType: session.userType,
        role: session.role,
        scope: session.scope,
      },
    [session, me.data],
  )

  const value = useMemo<AuthContextValue>(
    () => ({ user, isProfileLoading: session !== null && me.isPending, login, logout }),
    [user, session, me.isPending, login, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
