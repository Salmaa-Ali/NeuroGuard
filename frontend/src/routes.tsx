import type { RouteObject } from 'react-router-dom'
import { RequireAuth } from './auth/RequireAuth'
import { RequireRole } from './auth/RequireRole'
import { dashboard } from './features/dashboard/feature'
import { LoginPage } from './features/auth/LoginPage'
import { NotFoundPage } from './features/errors/NotFoundPage'
import { features } from './features'
import type { FeatureRoute } from './features/types'
import { AppShell } from './layout/AppShell'

/** Every feature route is wrapped in <RequireRole> with the access rule the feature declared. */
function toRoute(route: FeatureRoute): RouteObject {
  const element = (
    <RequireRole roles={route.access.roles} scopes={route.access.scopes}>
      {route.element}
    </RequireRole>
  )
  return route.path === '/' ? { index: true, element } : { path: route.path, element }
}

export const appRoutes: RouteObject[] = [
  { path: '/login', element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      {
        element: <AppShell />,
        children: [
          ...[dashboard, ...features].flatMap((f) => f.routes).map(toRoute),
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
]
