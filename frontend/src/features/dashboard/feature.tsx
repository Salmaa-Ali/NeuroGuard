import { ACCESS } from '../../auth/access'
import type { Feature } from '../types'
import { DashboardPage } from './DashboardPage'

/** The dashboard is shown to everyone and is always first in the menu (see layout/nav.ts). */
export const dashboard: Feature = {
  id: 'dashboard',
  routes: [{ path: '/', element: <DashboardPage />, access: ACCESS.anyone }],
}
