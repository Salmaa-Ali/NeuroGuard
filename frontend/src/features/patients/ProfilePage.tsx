import { PageHeader } from '@/components/app/PageHeader'
import { PatientDetails } from '@/components/app/PatientDetails'
import { useAuth } from '@/auth/context'

export function ProfilePage() {
  const { user } = useAuth()

  return (
    <>
      <PageHeader title="My profile" description="Your personal details on record." />
      {user && <PatientDetails patientId={user.id} />}
    </>
  )
}
