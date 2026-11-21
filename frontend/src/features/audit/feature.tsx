import { ACCESS } from '../../auth/access'
import type { Feature } from '../types'
import { AuditLogPage } from './AuditLogPage'

export const audit: Feature = {
  id: 'audit',
  routes: [
    {
      path: '/audit-log',
      element: <AuditLogPage />,
      access: ACCESS.auditLog,
      navLabel: 'Audit log',
    },
  ],
}
