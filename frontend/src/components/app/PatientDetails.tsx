import { usePatient } from '@/features/patients/api'
import { isNotFound } from '@/api/errors'

function Field({ label, value }: { label: string; value: string | null | undefined }) {
  return (
    <div className="px-4 py-3 sm:grid sm:grid-cols-3 sm:gap-4">
      <dt className="text-sm font-medium text-muted-foreground">{label}</dt>
      <dd className="mt-1 whitespace-pre-line text-sm sm:col-span-2 sm:mt-0">{value || '—'}</dd>
    </div>
  )
}

/** One patient. Database rules return the row only to the patient themself or allowed staff. */
export function PatientDetails({ patientId }: { patientId: string }) {
  const { data, isPending, error, refetch } = usePatient(patientId)

  if (isPending) return <p className="text-sm text-muted-foreground">Loading patient…</p>
  if (error) {
    if (isNotFound(error)) {
      return <p className="rounded-lg border border-border bg-card p-4 text-sm">Patient not found.</p>
    }
    return (
      <div className="rounded-lg border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
        {error instanceof Error ? error.message : 'Something went wrong.'}{' '}
        <button className="underline" onClick={() => void refetch()}>
          Retry
        </button>
      </div>
    )
  }
  if (!data) return <p className="rounded-lg border border-border bg-card p-4 text-sm">Patient not found.</p>

  return (
    <dl className="divide-y divide-border rounded-lg border border-border bg-card shadow-card">
      <Field label="Name" value={data.name} />
      <Field label="Patient number" value={data.patientNumber} />
      <Field label="Email" value={data.email} />
      <Field label="Date of birth" value={data.dateOfBirth} />
      <Field label="Phone" value={data.phone} />
      <Field label="Neurology history" value={data.neurologyHistory} />
    </dl>
  )
}
