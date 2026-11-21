import { ACCESS } from '../../auth/access'
import type { Feature } from '../types'
import { ScannersPage } from './ScannersPage'

export const scanners: Feature = {
  id: 'scanners',
  routes: [
    {
      path: '/scanners',
      element: <ScannersPage />,
      access: ACCESS.scanners,
      navLabel: 'Scanners & QA',
    },
  ],
}
