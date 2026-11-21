import { ACCESS } from '../../auth/access'
import type { Feature } from '../types'
import { AppointmentsPage } from './AppointmentsPage'
import { MyAppointmentsPage } from './MyAppointmentsPage'

export const appointments: Feature = {
  id: 'appointments',
  routes: [
    {
      path: '/appointments',
      element: <AppointmentsPage />,
      access: ACCESS.appointments,
      navLabel: 'Appointments',
    },
    {
      path: '/my/appointments',
      element: <MyAppointmentsPage />,
      access: ACCESS.patient,
      navLabel: 'My appointments',
    },
  ],
}
