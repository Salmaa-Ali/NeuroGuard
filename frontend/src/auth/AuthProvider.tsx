import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect, useMemo, useSyncExternalStore, type ReactNode } from 'react'
import type { LoginInput, LoginResult } from '../api/types'
import { fetchMe, loginRequest, registerPatientRequest, type RegisterPatientInput } from './api'
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

  // One login form for everybody: try the staff endpoint, and only if it says "invalid email or
  // password" (401) try the patient endpoint. Any other failure (400, network, 500) stops right away.
  const login = useCallback(async (credentials: LoginInput) => {
    const result: LoginResult = await loginRequest(credentials)
    setSession(createSession(result, credentials.email))
  }, [])

  const registerPatient = useCallback(async (input: RegisterPatientInput) => {
    const result = await registerPatientRequest(input)
    setSession(createSession(result, input.email))
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
    () => ({
      user,
      isProfileLoading: session !== null && me.isPending,
      login,
      registerPatient,
      logout,
    }),
    [user, session, me.isPending, login, registerPatient, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
