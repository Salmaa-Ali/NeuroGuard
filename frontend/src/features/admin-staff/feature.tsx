import { ACCESS } from '../../auth/access'
import type { Feature } from '../types'
import { StaffPage } from './StaffPage'

export const admin: Feature = {
  id: 'admin-staff',
  routes: [
    {
      path: '/admin/staff',
      element: <StaffPage />,
      access: ACCESS.admin,
      navLabel: 'Staff management',
    },
  ],
}
