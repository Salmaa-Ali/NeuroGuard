import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './context'

/** Layout route: anyone not logged in is sent to /login (and back afterwards). */
export function RequireAuth() {
  const { user } = useAuth()
  const location = useLocation()
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  return <Outlet />
}
