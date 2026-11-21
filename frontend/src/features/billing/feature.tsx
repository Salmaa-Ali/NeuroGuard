import { ACCESS } from '../../auth/access'
import type { Feature } from '../types'
import { InvoicesPage } from './InvoicesPage'
import { MyInvoicesPage } from './MyInvoicesPage'
import { PaymentsPage } from './PaymentsPage'

export const billing: Feature = {
  id: 'billing',
  routes: [
    { path: '/invoices', element: <InvoicesPage />, access: ACCESS.invoices, navLabel: 'Invoices' },
    { path: '/payments', element: <PaymentsPage />, access: ACCESS.payments, navLabel: 'Payments' },
    {
      path: '/my/invoices',
      element: <MyInvoicesPage />,
      access: ACCESS.patient,
      navLabel: 'My invoices',
    },
  ],
}
