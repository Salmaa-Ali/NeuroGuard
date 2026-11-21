import { Link, useParams } from 'react-router-dom'
import { PageHeader } from '../../components/PageHeader'
import { PatientDetails } from './PatientDetails'

/** Route: /patients/:patientId (clinical staff). */
export function PatientSummaryPage() {
  const { patientId = '' } = useParams<{ patientId: string }>()
  return (
    <>
      <PageHeader
        title="Patient summary"
        actions={
          <Link to="/patients" className="text-sm font-medium text-accent-600 hover:underline">
            ← Patients
          </Link>
        }
      />
      <PatientDetails patientId={patientId} />
    </>
  )
}
