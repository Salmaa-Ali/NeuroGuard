import { ACCESS } from '../../auth/access'
import type { Feature } from '../types'
import { ClinicalPage } from './ClinicalPage'
import { MyPrescriptionsPage } from './MyPrescriptionsPage'

export const clinical: Feature = {
  id: 'clinical',
  routes: [
    { path: '/clinical', element: <ClinicalPage />, access: ACCESS.clinical, navLabel: 'Clinical' },
    {
      path: '/my/prescriptions',
      element: <MyPrescriptionsPage />,
      access: ACCESS.patient,
      navLabel: 'My prescriptions',
    },
  ],
}
