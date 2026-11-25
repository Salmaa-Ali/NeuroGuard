import { Link, useParams } from 'react-router-dom'
import { PageHeader } from '@/components/app/PageHeader'
import { PatientDetails } from '@/components/app/PatientDetails'

export function PatientSummaryPage() {
  const { patientId, id } = useParams<{ patientId?: string; id?: string }>()
  const actualId = patientId ?? id

  if (!actualId) {
    return <p className="text-sm text-destructive">No patient ID provided.</p>
  }

  return (
    <>
      <PageHeader
        title="Patient summary"
        actions={
          <Link to="/patients" className="text-sm font-medium text-primary hover:underline">
            ← Patients
          </Link>
        }
      />
      <PatientDetails patientId={actualId} />
    </>
  )
}
