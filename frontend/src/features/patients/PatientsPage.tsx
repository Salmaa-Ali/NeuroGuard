import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { PlaceholderPage } from '@/components/app/PlaceholderPage'

const idSchema = z
  .string()
  .trim()
  .min(1, 'Enter a patient ID')
  .pipe(z.string().uuid('Enter a valid patient ID (a UUID)'))

export function PatientsPage() {
  const navigate = useNavigate()
  const [value, setValue] = useState('')
  const [error, setError] = useState<string | null>(null)

  function submit(e: FormEvent) {
    e.preventDefault()
    const r = idSchema.safeParse(value)
    if (!r.success) {
      return setError(r.error.issues[0]?.message ?? 'Invalid ID')
    }
    navigate(`/patients/${r.data}`)
  }

  return (
    <>
      <PlaceholderPage
        title="Patients"
        description="Search and browse patients. For now, open a patient directly by ID below."
        phase={2}
      />
      <form
        onSubmit={submit}
        noValidate
        className="mt-6 max-w-md space-y-3 rounded-lg border border-border bg-card p-5 shadow-card"
      >
        <h2 className="text-sm font-semibold">Open a patient by ID</h2>
        <div className="space-y-1.5">
          <Label htmlFor="pid">Patient ID</Label>
          <Input
            id="pid"
            placeholder="00000000-0000-0000-0000-000000000000"
            className="font-mono"
            value={value}
            onChange={(e) => setValue(e.target.value)}
          />
          {error && <p className="text-sm text-destructive">{error}</p>}
        </div>
        <Button type="submit">Open patient</Button>
      </form>
    </>
  )
}
