import { useAuth } from '../../auth/context'
import { PageHeader } from '../../components/PageHeader'
import { PatientDetails } from './PatientDetails'

/** "My profile" for patients: loads /api/patients/{own id}, the id being the token's "sub". */
export function ProfilePage() {
  const { user } = useAuth()
  if (!user) return null
  return (
    <>
      <PageHeader title="My profile" description="Your personal details on record." />
      <PatientDetails patientId={user.id} />
    </>
  )
}
