import { useState, type FormEvent } from 'react'
import { toast } from 'sonner'
import { Pencil, Plus, Trash2 } from 'lucide-react'
import { z } from 'zod'
import { PageHeader } from '@/components/app/PageHeader'
import { InvoiceList } from '@/components/app/InvoiceList'
import { DataCard, EmptyState, TableSkeleton, td, th } from '@/components/app/DataTable'
import { day, money } from '@/components/app/StatusBadge'
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
import { toApiError } from '@/api/errors'
import { usePatients } from '@/features/patients/api'
import { cn } from '@/lib/utils'
import {
  useCreateInvoice,
  useCreateService,
  useServices,
  useUpdateService,
  type ServiceRecord,
} from './api'

const uuidSchema = z.string().trim().uuid('Enter a valid UUID')

export function InvoicesPage() {
  const [tab, setTab] = useState<'invoices' | 'catalogue'>('invoices')
  const [creatingInvoice, setCreatingInvoice] = useState(false)
  const [serviceModal, setServiceModal] = useState<{ mode: 'create' } | { mode: 'edit'; service: ServiceRecord } | null>(null)

  const servicesQuery = useServices(true)

  return (
    <>
      <PageHeader
        eyebrow="Billing"
        title="Invoices & Service Catalogue"
        description="Issue invoices, process simulated payments, and maintain billable services."
        actions={
          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={() => setServiceModal({ mode: 'create' })}>
              <Plus className="h-4 w-4" />
              Add service
            </Button>
            <Button onClick={() => setCreatingInvoice(true)}>
              <Plus className="h-4 w-4" />
              Create invoice
            </Button>
          </div>
        }
      />

      <Tabs value={tab} onValueChange={(v) => setTab(v as 'invoices' | 'catalogue')} className="space-y-6">
        <TabsList>
          <TabsTrigger value="invoices">Invoices</TabsTrigger>
          <TabsTrigger value="catalogue">Service catalogue</TabsTrigger>
        </TabsList>

        <TabsContent value="invoices">
          <InvoiceList showPatient />
        </TabsContent>

        <TabsContent value="catalogue">
          <DataCard
            title="Billable services"
            actions={
              <Button size="sm" onClick={() => setServiceModal({ mode: 'create' })}>
                <Plus className="h-3.5 w-3.5" />
                Add service
              </Button>
            }
          >
            {servicesQuery.isLoading ? (
              <TableSkeleton />
            ) : servicesQuery.error ? (
              <EmptyState text="Couldn't load service catalogue." />
            ) : (servicesQuery.data ?? []).length === 0 ? (
              <EmptyState text="No services in the catalogue yet. Add one to start billing." />
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-muted/40">
                    <tr>
                      <th className={th}>Service name</th>
                      <th className={cn(th, 'text-right')}>Price (EGP)</th>
                      <th className={th}>Updated</th>
                      <th className={cn(th, 'text-right')}>Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border">
                    {(servicesQuery.data ?? []).map((s) => (
                      <tr key={s.id} className="transition-colors hover:bg-muted/30">
                        <td className={cn(td, 'font-medium')}>{s.name}</td>
                        <td className={cn(td, 'text-right font-medium tabular-nums')}>
                          {money(Number(s.price))}
                        </td>
                        <td className={cn(td, 'text-muted-foreground')}>{day(s.updatedAt)}</td>
                        <td className={cn(td, 'text-right')}>
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => setServiceModal({ mode: 'edit', service: s })}
                          >
                            <Pencil className="h-3.5 w-3.5" />
                            Edit
                          </Button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </DataCard>
        </TabsContent>
      </Tabs>

      <CreateInvoiceDialog
        open={creatingInvoice}
        onOpenChange={setCreatingInvoice}
        services={servicesQuery.data ?? []}
      />

      <ServiceDialog
        modal={serviceModal}
        onClose={() => setServiceModal(null)}
      />
    </>
  )
}

interface DraftItem {
  serviceId: string
  quantity: string
  icd10Code: string
}

function CreateInvoiceDialog({
  open,
  onOpenChange,
  services,
}: {
  open: boolean
  onOpenChange: (open: boolean) => void
  services: ServiceRecord[]
}) {
  const createInvoice = useCreateInvoice()
  const patientsQuery = usePatients('', 0, 100)
  const [patientId, setPatientId] = useState('')
  const [appointmentId, setAppointmentId] = useState('')
  const [items, setItems] = useState<DraftItem[]>([{ serviceId: '', quantity: '1', icd10Code: '' }])
  const [error, setError] = useState<string | null>(null)

  const estimatedTotal = items.reduce((sum, item) => {
    const svc = services.find((s) => s.id === item.serviceId)
    const qty = Number(item.quantity) || 0
    return sum + (svc ? Number(svc.price) * qty : 0)
  }, 0)

  function addItem() {
    setItems((prev) => [...prev, { serviceId: '', quantity: '1', icd10Code: '' }])
  }

  function removeItem(idx: number) {
    setItems((prev) => prev.filter((_, i) => i !== idx))
  }

  function updateItem(idx: number, patch: Partial<DraftItem>) {
    setItems((prev) => prev.map((it, i) => (i === idx ? { ...it, ...patch } : it)))
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)

    if (!patientId) {
      return setError('Select a patient.')
    }

    let parsedAppt: string | null = null
    if (appointmentId.trim()) {
      const aCheck = uuidSchema.safeParse(appointmentId)
      if (!aCheck.success) {
        return setError('Appointment ID must be a valid UUID (or leave blank).')
      }
      parsedAppt = aCheck.data
    }

    if (items.length === 0 || items.some((i) => !i.serviceId)) {
      return setError('Select a service for every line item.')
    }

    const payloadItems = items.map((i) => ({
      serviceId: i.serviceId,
      quantity: Math.max(1, Math.min(1000, Number(i.quantity) || 1)),
      icd10Code: i.icd10Code.trim() ? i.icd10Code.trim().toUpperCase() : null,
    }))

    try {
      await createInvoice.mutateAsync({
        patientId,
        appointmentId: parsedAppt,
        items: payloadItems,
      })
      toast.success('Invoice created')
      setPatientId('')
      setAppointmentId('')
      setItems([{ serviceId: '', quantity: '1', icd10Code: '' }])
      onOpenChange(false)
    } catch (err) {
      const apiErr = toApiError(err)
      setError(apiErr.message || 'Failed to create invoice.')
    }
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Create invoice</DialogTitle>
          <DialogDescription>
            Unit prices and totals are computed automatically from the service catalogue.
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

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="space-y-1.5">
              <Label htmlFor="inv-patient">Patient</Label>
              <select
                id="inv-patient"
                value={patientId}
                onChange={(e) => setPatientId(e.target.value)}
                className="h-9 w-full rounded-md border border-input bg-card px-2.5 text-sm"
              >
                <option value="">
                  {patientsQuery.isLoading ? 'Loading patients…' : 'Choose a patient…'}
                </option>
                {(patientsQuery.data?.content ?? []).map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.patientNumber} — {p.name}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-1.5">
              <Label htmlFor="inv-appt">Appointment ID (optional)</Label>
              <Input
                id="inv-appt"
                className="font-mono text-xs"
                placeholder="Optional UUID"
                value={appointmentId}
                onChange={(e) => setAppointmentId(e.target.value)}
              />
            </div>
          </div>

          <div className="space-y-2.5">
            <div className="flex items-center justify-between">
              <Label>Line items</Label>
              <Button type="button" variant="outline" size="sm" onClick={addItem}>
                <Plus className="h-3.5 w-3.5" />
                Add item
              </Button>
            </div>

            {services.length === 0 ? (
              <p className="rounded-lg border border-dashed border-border p-4 text-center text-sm text-muted-foreground">
                No services available. Add a service to the catalogue first.
              </p>
            ) : (
              <div className="space-y-2">
                {items.map((item, idx) => (
                  <div
                    key={idx}
                    className="grid items-end gap-2 rounded-xl border border-border bg-muted/20 p-3 sm:grid-cols-[1fr_90px_120px_auto]"
                  >
                    <div className="space-y-1">
                      <Label className="text-xs text-muted-foreground">Service</Label>
                      <select
                        value={item.serviceId}
                        onChange={(e) => updateItem(idx, { serviceId: e.target.value })}
                        className="h-9 w-full rounded-md border border-input bg-card px-2.5 text-sm"
                      >
                        <option value="">Select service…</option>
                        {services.map((s) => (
                          <option key={s.id} value={s.id}>
                            {s.name} ({money(Number(s.price))})
                          </option>
                        ))}
                      </select>
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs text-muted-foreground">Qty</Label>
                      <Input
                        type="number"
                        min={1}
                        max={1000}
                        value={item.quantity}
                        onChange={(e) => updateItem(idx, { quantity: e.target.value })}
                      />
                    </div>
                    <div className="space-y-1">
                      <Label className="text-xs text-muted-foreground">ICD-10 (opt)</Label>
                      <Input
                        placeholder="e.g. G40909"
                        className="font-mono text-xs"
                        maxLength={10}
                        value={item.icd10Code}
                        onChange={(e) => updateItem(idx, { icd10Code: e.target.value })}
                      />
                    </div>
                    <Button
                      type="button"
                      variant="ghost"
                      size="icon"
                      disabled={items.length === 1}
                      onClick={() => removeItem(idx)}
                      aria-label="Remove item"
                    >
                      <Trash2 className="h-4 w-4 text-muted-foreground" />
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="flex items-center justify-between border-t border-border pt-3">
            <div className="text-sm">
              <span className="text-muted-foreground">Estimated total: </span>
              <span className="font-semibold tabular-nums">{money(estimatedTotal)}</span>
            </div>
            <div className="flex gap-2">
              <Button type="button" variant="outline" onClick={() => onOpenChange(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={createInvoice.isPending || services.length === 0}>
                {createInvoice.isPending ? 'Creating…' : 'Create invoice'}
              </Button>
            </div>
          </div>
        </form>
      </DialogContent>
    </Dialog>
  )
}

function ServiceDialog({
  modal,
  onClose,
}: {
  modal: { mode: 'create' } | { mode: 'edit'; service: ServiceRecord } | null
  onClose: () => void
}) {
  const isEdit = modal?.mode === 'edit'
  const initialService = isEdit ? modal.service : null

  return (
    <Dialog open={modal !== null} onOpenChange={(o) => !o && onClose()}>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>{isEdit ? 'Edit service' : 'Add service'}</DialogTitle>
          <DialogDescription>
            {isEdit
              ? 'Existing invoices keep the price they were billed at.'
              : 'Add a billable service to the NeuroGuard catalogue.'}
          </DialogDescription>
        </DialogHeader>
        {modal && (
          <ServiceForm
            key={initialService?.id ?? 'new'}
            initialService={initialService}
            onDone={onClose}
          />
        )}
      </DialogContent>
    </Dialog>
  )
}

function ServiceForm({
  initialService,
  onDone,
}: {
  initialService: ServiceRecord | null
  onDone: () => void
}) {
  const createSvc = useCreateService()
  const updateSvc = useUpdateService()
  const [name, setName] = useState(initialService?.name ?? '')
  const [price, setPrice] = useState(initialService ? String(initialService.price) : '')
  const [error, setError] = useState<string | null>(null)

  const busy = createSvc.isPending || updateSvc.isPending

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)

    const trimmedName = name.trim()
    if (!trimmedName) return setError('Service name is required.')

    const numPrice = Number(price)
    if (Number.isNaN(numPrice) || numPrice < 0) {
      return setError('Price must be 0 or more.')
    }

    try {
      if (initialService) {
        await updateSvc.mutateAsync({
          id: initialService.id,
          input: { name: trimmedName, price: numPrice },
        })
        toast.success('Service updated')
      } else {
        await createSvc.mutateAsync({ name: trimmedName, price: numPrice })
        toast.success('Service added')
      }
      onDone()
    } catch (err) {
      setError(toApiError(err).message || 'Failed to save service.')
    }
  }

  return (
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
        <Label htmlFor="svc-name">Service name</Label>
        <Input
          id="svc-name"
          placeholder="e.g. Brain MRI with Contrast"
          value={name}
          onChange={(e) => setName(e.target.value)}
        />
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="svc-price">Price (EGP)</Label>
        <Input
          id="svc-price"
          type="number"
          step="0.01"
          min="0"
          placeholder="1500.00"
          value={price}
          onChange={(e) => setPrice(e.target.value)}
        />
      </div>
      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onDone}>
          Cancel
        </Button>
        <Button type="submit" disabled={busy}>
          {busy ? 'Saving…' : initialService ? 'Save changes' : 'Add service'}
        </Button>
      </div>
    </form>
  )
}
