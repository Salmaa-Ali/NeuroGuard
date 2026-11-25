import { useState, type FormEvent } from 'react'
import { useSearchParams } from 'react-router-dom'
import { toast } from 'sonner'
import { BookOpen, Eye, FilePlus2, Pill, Plus, Search, Stethoscope } from 'lucide-react'
import { z } from 'zod'
import { PageHeader } from '@/components/app/PageHeader'
import { DataCard, EmptyState, TableSkeleton, td, th } from '@/components/app/DataTable'
import { StatusBadge, day } from '@/components/app/StatusBadge'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Textarea } from '@/components/ui/textarea'
import { toApiError } from '@/api/errors'
import { useAuth } from '@/auth/context'
import { usePatient } from '@/features/patients/api'
import { cn } from '@/lib/utils'
import {
  useAddPrescription,
  useCreateDiagnosis,
  useCreateTreatmentPlan,
  useDiagnosis,
  useIcd10Search,
  usePatientDiagnoses,
  usePatientTreatmentPlans,
  type TreatmentPlanRecord,
} from './api'

const uuidSchema = z
  .string()
  .trim()
  .min(1, 'Enter a patient ID')
  .pipe(z.string().uuid('Enter a valid patient ID (a UUID)'))

export function ClinicalPage() {
  const { user } = useAuth()
  const [searchParams, setSearchParams] = useSearchParams()
  const initialPatientId = searchParams.get('patientId') ?? ''
  const [inputValue, setInputValue] = useState(initialPatientId)
  const [activePatientId, setActivePatientId] = useState<string | null>(() => {
    const parsed = uuidSchema.safeParse(initialPatientId)
    return parsed.success ? parsed.data : null
  })
  const [idError, setIdError] = useState<string | null>(null)
  const [tab, setTab] = useState<'diagnoses' | 'plans' | 'icd10'>('diagnoses')

  const [addingDiagnosis, setAddingDiagnosis] = useState(false)
  const [viewingDiagnosisId, setViewingDiagnosisId] = useState<string | null>(null)
  const [addingPlan, setAddingPlan] = useState(false)
  const [prescriptionTarget, setPrescriptionTarget] = useState<TreatmentPlanRecord | null>(null)

  const canWrite = user?.scope === 'NEUROLOGIST'
  const patientQuery = usePatient(activePatientId ?? '')
  const diagnosesQuery = usePatientDiagnoses(activePatientId)
  const plansQuery = usePatientTreatmentPlans(activePatientId)

  function handleSelectPatient(e: FormEvent) {
    e.preventDefault()
    const parsed = uuidSchema.safeParse(inputValue)
    if (!parsed.success) {
      setIdError(parsed.error.issues[0]?.message ?? 'Invalid ID')
      return
    }
    setIdError(null)
    setActivePatientId(parsed.data)
    setSearchParams({ patientId: parsed.data })
  }

  return (
    <>
      <PageHeader
        eyebrow="Clinical"
        title="Clinical records"
        description="Diagnoses (ICD-10), treatment plans, and prescriptions for your patients."
        actions={
          activePatientId && canWrite ? (
            <div className="flex items-center gap-2">
              <Button variant="outline" onClick={() => setAddingPlan(true)}>
                <FilePlus2 className="h-4 w-4" />
                New treatment plan
              </Button>
              <Button onClick={() => setAddingDiagnosis(true)}>
                <Plus className="h-4 w-4" />
                Add diagnosis
              </Button>
            </div>
          ) : null
        }
      />

      <div className="mb-6 rounded-2xl border border-border bg-card p-5 shadow-card">
        <form onSubmit={handleSelectPatient} noValidate className="flex flex-wrap items-end gap-3">
          <div className="min-w-[260px] flex-1 space-y-1.5">
            <Label htmlFor="clinical-pid">Patient ID (UUID)</Label>
            <Input
              id="clinical-pid"
              placeholder="00000000-0000-0000-0000-000000000000"
              className="font-mono text-sm"
              value={inputValue}
              onChange={(e) => setInputValue(e.target.value)}
            />
            {idError && <p className="text-xs text-destructive">{idError}</p>}
          </div>
          <Button type="submit">Load patient record</Button>
        </form>

        {activePatientId && (
          <div className="mt-4 flex flex-wrap items-center justify-between gap-2 border-t border-border pt-3 text-sm">
            {patientQuery.isLoading ? (
              <span className="text-muted-foreground">Loading patient summary…</span>
            ) : patientQuery.data ? (
              <div className="flex flex-wrap items-center gap-3">
                <span className="font-semibold">{patientQuery.data.name}</span>
                <span className="rounded-md bg-muted px-2 py-0.5 font-mono text-xs text-muted-foreground">
                  {patientQuery.data.patientNumber}
                </span>
                <span className="text-xs text-muted-foreground">{patientQuery.data.email}</span>
              </div>
            ) : (
              <span className="text-muted-foreground">
                Active patient: <span className="font-mono text-xs">{activePatientId}</span>
              </span>
            )}
          </div>
        )}
      </div>

      <Tabs
        value={tab}
        onValueChange={(v) => setTab(v as 'diagnoses' | 'plans' | 'icd10')}
        className="space-y-6"
      >
        <TabsList>
          <TabsTrigger value="diagnoses" className="gap-1.5">
            <Stethoscope className="h-3.5 w-3.5" />
            Diagnoses
          </TabsTrigger>
          <TabsTrigger value="plans" className="gap-1.5">
            <Pill className="h-3.5 w-3.5" />
            Treatment plans & prescriptions
          </TabsTrigger>
          <TabsTrigger value="icd10" className="gap-1.5">
            <BookOpen className="h-3.5 w-3.5" />
            ICD-10 reference
          </TabsTrigger>
        </TabsList>

        <TabsContent value="diagnoses">
          {!activePatientId ? (
            <DataCard title="Patient diagnoses">
              <EmptyState text="Enter a patient ID above to view or record diagnoses." />
            </DataCard>
          ) : (
            <DataCard
              title="Diagnoses"
              actions={
                canWrite && (
                  <Button size="sm" onClick={() => setAddingDiagnosis(true)}>
                    <Plus className="h-3.5 w-3.5" />
                    Add diagnosis
                  </Button>
                )
              }
            >
              {diagnosesQuery.isLoading ? (
                <TableSkeleton />
              ) : diagnosesQuery.error ? (
                <EmptyState text={toApiError(diagnosesQuery.error).message || "Couldn't load diagnoses."} />
              ) : (diagnosesQuery.data ?? []).length === 0 ? (
                <EmptyState text="No diagnoses recorded for this patient yet." />
              ) : (
                <div className="overflow-x-auto">
                  <table className="w-full">
                    <thead className="bg-muted/40">
                      <tr>
                        <th className={th}>ICD-10</th>
                        <th className={th}>Description</th>
                        <th className={th}>Diagnosed</th>
                        <th className={th}>Neurologist</th>
                        <th className={cn(th, 'text-right')}>Actions</th>
                      </tr>
                    </thead>
                    <tbody className="divide-y divide-border">
                      {(diagnosesQuery.data ?? []).map((d) => (
                        <tr key={d.id} className="transition-colors hover:bg-muted/30">
                          <td className={cn(td, 'font-mono text-xs font-semibold text-primary')}>
                            {d.icd10Code}
                          </td>
                          <td className={td}>
                            <p className="font-medium">{d.icd10Description}</p>
                            {d.notes && (
                              <p className="mt-0.5 line-clamp-1 text-xs text-muted-foreground">
                                {d.notes}
                              </p>
                            )}
                          </td>
                          <td className={td}>{day(d.diagnosedAt)}</td>
                          <td className={td}>{d.staffName}</td>
                          <td className={cn(td, 'text-right')}>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => setViewingDiagnosisId(d.id)}
                            >
                              <Eye className="h-3.5 w-3.5" />
                              View
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              )}
            </DataCard>
          )}
        </TabsContent>

        <TabsContent value="plans">
          {!activePatientId ? (
            <DataCard title="Treatment plans">
              <EmptyState text="Enter a patient ID above to view or create treatment plans." />
            </DataCard>
          ) : (
            <DataCard
              title="Treatment plans & prescriptions"
              actions={
                canWrite && (
                  <Button size="sm" onClick={() => setAddingPlan(true)}>
                    <Plus className="h-3.5 w-3.5" />
                    New plan
                  </Button>
                )
              }
            >
              {plansQuery.isLoading ? (
                <TableSkeleton />
              ) : plansQuery.error ? (
                <EmptyState text={toApiError(plansQuery.error).message || "Couldn't load treatment plans."} />
              ) : (plansQuery.data ?? []).length === 0 ? (
                <EmptyState text="No treatment plans recorded for this patient yet." />
              ) : (
                <div className="divide-y divide-border">
                  {(plansQuery.data ?? []).map((plan) => (
                    <div key={plan.id} className="space-y-3 p-5">
                      <div className="flex flex-wrap items-start justify-between gap-3">
                        <div className="space-y-1">
                          <div className="flex flex-wrap items-center gap-2">
                            <StatusBadge status={plan.endDate ? 'COMPLETED' : 'ACTIVE'} />
                            <span className="text-xs text-muted-foreground">
                              {day(plan.startDate)}
                              {plan.endDate ? ` — ${day(plan.endDate)}` : ' — Ongoing'}
                            </span>
                            <span className="text-xs text-muted-foreground">· Dr. {plan.staffName}</span>
                          </div>
                          <p className="text-sm font-medium">{plan.description}</p>
                        </div>
                        {canWrite && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => setPrescriptionTarget(plan)}
                          >
                            <Plus className="h-3.5 w-3.5" />
                            Add prescription
                          </Button>
                        )}
                      </div>

                      {plan.prescriptions.length === 0 ? (
                        <p className="rounded-lg border border-dashed border-border px-3 py-2 text-xs text-muted-foreground">
                          No prescriptions added to this plan yet.
                        </p>
                      ) : (
                        <div className="space-y-1.5 rounded-xl border border-border bg-muted/20 p-3">
                          <p className="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">
                            Prescriptions ({plan.prescriptions.length})
                          </p>
                          <ul className="divide-y divide-border/60">
                            {plan.prescriptions.map((rx) => (
                              <li
                                key={rx.id}
                                className="flex flex-wrap items-center justify-between gap-2 py-2 text-sm first:pt-1 last:pb-0"
                              >
                                <span className="font-medium">{rx.medicationDetails}</span>
                                <span className="text-xs text-muted-foreground">
                                  {rx.staffName} · {day(rx.createdAt)}
                                </span>
                              </li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </DataCard>
          )}
        </TabsContent>

        <TabsContent value="icd10">
          <Icd10Browser />
        </TabsContent>
      </Tabs>

      {activePatientId && (
        <>
          <CreateDiagnosisDialog
            open={addingDiagnosis}
            onOpenChange={setAddingDiagnosis}
            patientId={activePatientId}
          />
          <CreateTreatmentPlanDialog
            open={addingPlan}
            onOpenChange={setAddingPlan}
            patientId={activePatientId}
          />
          <AddPrescriptionDialog
            plan={prescriptionTarget}
            patientId={activePatientId}
            onClose={() => setPrescriptionTarget(null)}
          />
        </>
      )}

      <DiagnosisDetailDialog
        diagnosisId={viewingDiagnosisId}
        onClose={() => setViewingDiagnosisId(null)}
      />
    </>
  )
}

function Icd10Browser() {
  const [query, setQuery] = useState('')
  const icdQuery = useIcd10Search(query, 0, 25)

  return (
    <DataCard
      title="ICD-10 Code Reference"
      actions={
        <div className="relative w-64">
          <Search className="pointer-events-none absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search code or description…"
            className="h-9 pl-8 text-sm"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
          />
        </div>
      }
    >
      {icdQuery.isLoading ? (
        <TableSkeleton />
      ) : icdQuery.error ? (
        <EmptyState text="Couldn't load ICD-10 codes." />
      ) : (icdQuery.data?.content ?? []).length === 0 ? (
        <EmptyState text="No matching ICD-10 codes found." />
      ) : (
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead className="bg-muted/40">
              <tr>
                <th className={th}>Code</th>
                <th className={th}>Category</th>
                <th className={th}>Description</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border">
              {(icdQuery.data?.content ?? []).map((item) => (
                <tr key={item.code} className="transition-colors hover:bg-muted/30">
                  <td className={cn(td, 'font-mono text-xs font-semibold text-primary')}>
                    {item.code}
                  </td>
                  <td className={cn(td, 'font-mono text-xs text-muted-foreground')}>
                    {item.category}
                  </td>
                  <td className={td}>{item.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </DataCard>
  )
}

function CreateDiagnosisDialog({
  open,
  onOpenChange,
  patientId,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  patientId: string
}) {
  const createDiag = useCreateDiagnosis(patientId)
  const [search, setSearch] = useState('')
  const [icd10Code, setIcd10Code] = useState('')
  const [notes, setNotes] = useState('')
  const [error, setError] = useState<string | null>(null)

  const icdResults = useIcd10Search(search, 0, 10)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    const code = icd10Code.trim()
    if (!code) return setError('Select or enter an ICD-10 code.')

    try {
      await createDiag.mutateAsync({
        icd10Code: code,
        notes: notes.trim() ? notes.trim() : null,
      })
      toast.success('Diagnosis recorded')
      setIcd10Code('')
      setNotes('')
      setSearch('')
      onOpenChange(false)
    } catch (err) {
      setError(toApiError(err).message || 'Failed to record diagnosis.')
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-lg">
        <DialogHeader>
          <DialogTitle>Record diagnosis</DialogTitle>
          <DialogDescription>Search ICD-10 codes and add clinical notes.</DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} noValidate className="space-y-4">
          {error && (
            <p
              role="alert"
              className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
            >
              {error}
            </p>
          )}
          <div className="space-y-1.5">
            <Label htmlFor="icd-search">Search ICD-10</Label>
            <Input
              id="icd-search"
              placeholder="e.g. G40 or Epilepsy"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            {(icdResults.data?.content ?? []).length > 0 && (
              <div className="max-h-36 overflow-y-auto rounded-md border border-border bg-muted/20 p-1">
                {(icdResults.data?.content ?? []).map((c) => (
                  <button
                    key={c.code}
                    type="button"
                    onClick={() => setIcd10Code(c.code)}
                    className={cn(
                      'flex w-full items-start gap-2 rounded px-2 py-1.5 text-left text-xs transition-colors hover:bg-muted cursor-pointer',
                      icd10Code === c.code && 'bg-primary/10 text-primary',
                    )}
                  >
                    <span className="font-mono font-semibold">{c.code}</span>
                    <span className="line-clamp-1 text-muted-foreground">{c.description}</span>
                  </button>
                ))}
              </div>
            )}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="diag-code">Selected ICD-10 Code</Label>
            <Input
              id="diag-code"
              className="font-mono"
              placeholder="e.g. G40909"
              value={icd10Code}
              onChange={(e) => setIcd10Code(e.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="diag-notes">Clinical notes (optional)</Label>
            <Textarea
              id="diag-notes"
              rows={3}
              placeholder="Observations, MRI findings, seizure frequency…"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={createDiag.isPending}>
              {createDiag.isPending ? 'Saving…' : 'Save diagnosis'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function DiagnosisDetailDialog({
  diagnosisId,
  onClose,
}: {
  diagnosisId: string | null
  onClose: () => void
}) {
  const { data, isLoading, error } = useDiagnosis(diagnosisId)

  return (
    <Dialog open={diagnosisId !== null} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Diagnosis details</DialogTitle>
          <DialogDescription>
            {data ? `${data.icd10Code} · ${data.icd10Category}` : 'Loading…'}
          </DialogDescription>
        </DialogHeader>
        {isLoading ? (
          <TableSkeleton />
        ) : error ? (
          <p className="text-sm text-destructive">{toApiError(error).message}</p>
        ) : data ? (
          <div className="space-y-3 text-sm">
            <div>
              <p className="text-xs text-muted-foreground">Description</p>
              <p className="mt-0.5 font-medium">{data.icd10Description}</p>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <p className="text-xs text-muted-foreground">Diagnosed on</p>
                <p className="mt-0.5">{day(data.diagnosedAt)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Neurologist</p>
                <p className="mt-0.5">{data.staffName}</p>
              </div>
            </div>
            <div>
              <p className="text-xs text-muted-foreground">Notes</p>
              <p className="mt-1 whitespace-pre-line rounded-lg border border-border bg-muted/30 p-3">
                {data.notes || 'No notes provided.'}
              </p>
            </div>
          </div>
        ) : null}
      </DialogContent>
    </Dialog>
  )
}

function CreateTreatmentPlanDialog({
  open,
  onOpenChange,
  patientId,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  patientId: string
}) {
  const createPlan = useCreateTreatmentPlan(patientId)
  const [description, setDescription] = useState('')
  const [startDate, setStartDate] = useState(() => new Date().toISOString().slice(0, 10))
  const [endDate, setEndDate] = useState('')
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    if (!description.trim()) return setError('Description is required.')
    if (!startDate) return setError('Start date is required.')

    try {
      await createPlan.mutateAsync({
        description: description.trim(),
        startDate,
        endDate: endDate || null,
      })
      toast.success('Treatment plan created')
      setDescription('')
      setEndDate('')
      onOpenChange(false)
    } catch (err) {
      setError(toApiError(err).message || 'Failed to create treatment plan.')
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>New treatment plan</DialogTitle>
          <DialogDescription>Outline the care plan and start date.</DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} noValidate className="space-y-4">
          {error && (
            <p
              role="alert"
              className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
            >
              {error}
            </p>
          )}
          <div className="space-y-1.5">
            <Label htmlFor="plan-desc">Plan description</Label>
            <Textarea
              id="plan-desc"
              rows={3}
              placeholder="e.g. Seizure management & 6-month MRI follow-up"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="plan-start">Start date</Label>
              <Input
                id="plan-start"
                type="date"
                value={startDate}
                onChange={(e) => setStartDate(e.target.value)}
              />
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="plan-end">End date (optional)</Label>
              <Input
                id="plan-end"
                type="date"
                value={endDate}
                onChange={(e) => setEndDate(e.target.value)}
              />
            </div>
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
              Cancel
            </Button>
            <Button type="submit" disabled={createPlan.isPending}>
              {createPlan.isPending ? 'Creating…' : 'Create plan'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function AddPrescriptionDialog({
  plan,
  patientId,
  onClose,
}: {
  plan: TreatmentPlanRecord | null
  patientId: string
  onClose: () => void
}) {
  const addRx = useAddPrescription(patientId)
  const [medicationDetails, setMedicationDetails] = useState('')
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!plan) return
    setError(null)
    if (!medicationDetails.trim()) return setError('Medication details are required.')

    try {
      await addRx.mutateAsync({
        planId: plan.id,
        input: { medicationDetails: medicationDetails.trim() },
      })
      toast.success('Prescription added')
      setMedicationDetails('')
      onClose()
    } catch (err) {
      setError(toApiError(err).message || 'Failed to add prescription.')
    }
  }

  return (
    <Dialog open={plan !== null} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Add prescription</DialogTitle>
          <DialogDescription>
            {plan ? `For plan: ${plan.description}` : ''}
          </DialogDescription>
        </DialogHeader>
        <form onSubmit={submit} noValidate className="space-y-4">
          {error && (
            <p
              role="alert"
              className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
            >
              {error}
            </p>
          )}
          <div className="space-y-1.5">
            <Label htmlFor="rx-details">Medication details</Label>
            <Textarea
              id="rx-details"
              rows={3}
              placeholder="e.g. Levetiracetam 500 mg, twice daily, oral"
              value={medicationDetails}
              onChange={(e) => setMedicationDetails(e.target.value)}
            />
          </div>
          <div className="flex justify-end gap-2">
            <Button type="button" variant="outline" onClick={onClose}>
              Cancel
            </Button>
            <Button type="submit" disabled={addRx.isPending}>
              {addRx.isPending ? 'Adding…' : 'Add prescription'}
            </Button>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}
