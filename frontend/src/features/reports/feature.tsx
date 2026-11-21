import { ACCESS } from '../../auth/access'
import type { Feature } from '../types'
import { MyReportsPage } from './MyReportsPage'
import { ReportsPage } from './ReportsPage'

export const reports: Feature = {
  id: 'reports',
  routes: [
    { path: '/reports', element: <ReportsPage />, access: ACCESS.reports, navLabel: 'Reports' },
    {
      path: '/my/reports',
      element: <MyReportsPage />,
      access: ACCESS.patient,
      navLabel: 'My reports',
    },
  ],
}
