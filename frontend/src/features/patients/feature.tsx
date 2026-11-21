import { ACCESS } from '../../auth/access'
import type { Feature } from '../types'
import { PatientsPage } from './PatientsPage'
import { PatientSummaryPage } from './PatientSummaryPage'
import { ProfilePage } from './ProfilePage'

export const patients: Feature = {
  id: 'patients',
  routes: [
    {
      path: '/patients',
      element: <PatientsPage />,
      access: ACCESS.patientsMenu,
      navLabel: 'Patients',
    },
    // Not in the menu: reached from the list / lookup. The backend allows any of these scopes.
    { path: '/patients/:patientId', element: <PatientSummaryPage />, access: ACCESS.patientRecord },
    { path: '/profile', element: <ProfilePage />, access: ACCESS.patient, navLabel: 'My profile' },
  ],
}
