import { createContext, useContext } from 'react'
import type { LoginInput, PermissionScope, Role, UserType } from '../api/types'
import type { LoginKind } from './api'

export interface AuthUser {
  id: string
  /** Null until /api/me has answered. */
  name: string | null
  email: string
  userType: UserType
  role: Role
  scope: PermissionScope | null
}

export interface AuthContextValue {
  user: AuthUser | null
  /** True while the profile (name) is still loading. */
  isProfileLoading: boolean
  /** Throws the axios error on failure so the caller can show the backend's message. */
  login: (kind: LoginKind, credentials: LoginInput) => Promise<void>
  logout: () => void
}

export const AuthContext = createContext<AuthContextValue | null>(null)

export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext)
  if (!value) throw new Error('useAuth must be used inside <AuthProvider>')
  return value
}
