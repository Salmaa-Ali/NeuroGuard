import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { Button } from '../../components/Button'
import { Input } from '../../components/Input'
import { PlaceholderPage } from '../../components/PlaceholderPage'

const lookupSchema = z.object({
  patientId: z
    .string()
    .trim()
    .min(1, 'Enter a patient ID')
    .pipe(z.uuid('Enter a valid patient ID (a UUID)')),
})
type LookupValues = z.infer<typeof lookupSchema>

/** Until the patient list exists (Phase 2), staff can open a patient by ID. */
function PatientLookup() {
  const navigate = useNavigate()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LookupValues>({ resolver: zodResolver(lookupSchema) })

  return (
    <form
      onSubmit={handleSubmit(({ patientId }) => navigate(`/patients/${patientId}`))}
      noValidate
      className="mt-6 max-w-md rounded-lg border border-slate-200 bg-white p-5"
    >
      <h2 className="mb-3 text-sm font-semibold text-slate-900">Open a patient by ID</h2>
      <Input
        label="Patient ID"
        placeholder="00000000-0000-0000-0000-000000000000"
        error={errors.patientId?.message}
        {...register('patientId')}
      />
      <Button type="submit" className="mt-4">
        Open patient
      </Button>
    </form>
  )
}

export function PatientsPage() {
  return (
    <>
      <PlaceholderPage
        title="Patients"
        description="Search and browse patients. For now, open a patient directly by ID below."
        phase={2}
      />
      <PatientLookup />
    </>
  )
}
