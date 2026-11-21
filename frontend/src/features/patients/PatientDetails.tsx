import { isNotFound } from '../../api/errors'
import { ErrorState } from '../../components/ErrorState'
import { LoadingState } from '../../components/LoadingState'
import { usePatient } from './api'

function Field({ label, value }: { label: string; value: string | null }) {
  return (
    <div className="px-4 py-3 sm:grid sm:grid-cols-3 sm:gap-4">
      <dt className="text-sm font-medium text-slate-500">{label}</dt>
      <dd className="mt-1 whitespace-pre-line text-sm text-slate-900 sm:col-span-2 sm:mt-0">
        {value || '—'}
      </dd>
    </div>
  )
}

/** Loads and shows one patient. Shared by the staff view (/patients/:id) and a patient's own profile. */
export function PatientDetails({ patientId }: { patientId: string }) {
  const { data, isPending, error, refetch } = usePatient(patientId)

  if (isPending) return <LoadingState label="Loading patient…" />
  if (error) {
    if (isNotFound(error)) {
      return <ErrorState error={new Error('Patient not found.')} />
    }
    return <ErrorState error={error} onRetry={() => void refetch()} />
  }

  return (
    <dl className="divide-y divide-slate-100 rounded-lg border border-slate-200 bg-white">
      <Field label="Name" value={data.name} />
      <Field label="Patient number" value={data.patientNumber} />
      <Field label="Email" value={data.email} />
      <Field label="Date of birth" value={data.dateOfBirth} />
      <Field label="Phone" value={data.phone} />
      <Field label="Neurology history" value={data.neurologyHistory} />
    </dl>
  )
}
