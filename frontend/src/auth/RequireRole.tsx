import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import type { PermissionScope, Role } from '../api/types'
import { AccessDenied } from '../components/AccessDenied'
import { isAllowed } from './access'
import { useAuth } from './context'

interface RequireRoleProps {
  /** Allowed if the user's role is one of these... */
  roles?: readonly Role[]
  /** ...or their permission scope is one of these. Omit both to allow any logged-in user. */
  scopes?: readonly PermissionScope[]
  children: ReactNode
}

/**
 * Route guard. Not logged in -> /login. Logged in without permission -> the "no access" screen
 * (the user stays logged in). The backend still enforces everything; this just saves a round trip.
 */
export function RequireRole({ roles, scopes, children }: RequireRoleProps) {
  const { user } = useAuth()
  if (!user) return <Navigate to="/login" replace />
  if (!isAllowed(user, { roles, scopes })) return <AccessDenied />
  return <>{children}</>
}
