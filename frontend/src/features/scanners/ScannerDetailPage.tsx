import { useState, type FormEvent } from 'react'
import { Link, useParams } from 'react-router-dom'
import { toast } from 'sonner'
import { ClipboardCheck, Pencil, Plus, Trash2 } from 'lucide-react'
import { toApiError } from '@/api/errors'
import { SCANNER_STATUSES, type Accessory, type ScannerStatus } from '@/api/types'
import { ACCESS, isAllowed } from '@/auth/access'
import { useAuth } from '@/auth/context'
import { DataCard, EmptyState, TableSkeleton, td, th } from '@/components/app/DataTable'
import { PageHeader } from '@/components/app/PageHeader'
import { ErrorState } from '@/components/ErrorState'
import { Input } from '@/components/Input'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { cn } from '@/lib/utils'
import {
  useCalibration,
  useCalibrationHistory,
  useChangeScannerStatus,
  useDeleteAccessory,
  useSaveAccessory,
  useScanner,
  useUpdateScanner,
} from './api'
import { PLACEHOLDER_TEXT, SCANNER_STATUS_LABEL, formatDate, formatLimits } from './qa'
import { QaStatusBadge, ResultBadge, RiskScore } from './QaStatusBadge'
import { ScannerForm } from './ScannerForm'
import { ScannerQaPanel } from './ScannerQaPanel'

export function ScannerDetailPage() {
  const { scannerId = '' } = useParams()
  const { user } = useAuth()
  const canWrite = isAllowed(user, ACCESS.scannersWrite)
  const scanner = useScanner(scannerId)
  const [editing, setEditing] = useState(false)
  const update = useUpdateScanner(scannerId)
  const changeStatus = useChangeScannerStatus(scannerId)

  if (scanner.isPending) return <TableSkeleton />
  if (scanner.isError) return <ErrorState error={scanner.error} onRetry={() => scanner.refetch()} />
  const s = scanner.data

  async function onStatus(status: ScannerStatus) {
    try {
      await changeStatus.mutateAsync(status)
      toast.success(`Status set to ${SCANNER_STATUS_LABEL[status]}`)
    } catch (err) {
      toast.error(toApiError(err).message || 'Failed to change status')
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Scanners & QA"
        title={s.name}
        description={`${s.manufacturer} ${s.model} · ${s.fieldStrength} T`}
        actions={
          canWrite && (
            <>
              <Button variant="outline" onClick={() => setEditing(true)}>
                <Pencil className="h-4 w-4" />
                Edit scanner
              </Button>
              {s.status !== 'RETIRED' && (
                <Button asChild>
                  <Link to={`/scanners/${s.id}/calibrations/new`}>
                    <ClipboardCheck className="h-4 w-4" />
                    Record calibration
                  </Link>
                </Button>
              )}
            </>
          )
        }
      />
      <p className="-mt-6 mb-6 text-sm">
        <Link to="/scanners" className="text-primary hover:underline">
          ← All scanners
        </Link>
      </p>

      <div className="grid gap-6 lg:grid-cols-3">
        <DataCard title="Scanner">
          <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 p-5 text-sm">
            <dt className="text-muted-foreground">Serial number</dt>
            <dd className="font-mono text-xs">{s.serialNumber}</dd>
            <dt className="text-muted-foreground">Station</dt>
            <dd>{s.stationName ?? '—'}</dd>
            <dt className="text-muted-foreground">Location</dt>
            <dd>{s.location ?? '—'}</dd>
            <dt className="text-muted-foreground">Installed</dt>
            <dd>{formatDate(s.installDate)}</dd>
            <dt className="text-muted-foreground">QA interval</dt>
            <dd>{s.calibrationIntervalDays} days</dd>
            <dt className="text-muted-foreground">Status</dt>
            <dd>
              {canWrite ? (
                <select
                  aria-label="Scanner status"
                  value={s.status}
                  disabled={changeStatus.isPending}
                  onChange={(e) => onStatus(e.target.value as ScannerStatus)}
                  className="rounded-md border border-input bg-card px-2 py-1"
                >
                  {SCANNER_STATUSES.map((st) => (
                    <option key={st} value={st}>
                      {SCANNER_STATUS_LABEL[st]}
                    </option>
                  ))}
                </select>
              ) : (
                SCANNER_STATUS_LABEL[s.status]
              )}
            </dd>
            <dt className="text-muted-foreground">QA status</dt>
            <dd>
              <QaStatusBadge status={s.qaStatus} />
            </dd>
          </dl>
        </DataCard>

        <ScannerQaPanel scannerId={s.id} />

        <Coils scannerId={s.id} coils={s.accessories} canWrite={canWrite} />
      </div>

      <div className="mt-6">
        <History scannerId={s.id} />
      </div>

      <Dialog open={editing} onOpenChange={setEditing}>
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>Edit scanner</DialogTitle>
          </DialogHeader>
          <ScannerForm
            initial={s}
            submitLabel="Save"
            onCancel={() => setEditing(false)}
            onSubmit={async (input) => {
              await update.mutateAsync(input)
              toast.success('Scanner updated')
              setEditing(false)
            }}
          />
        </DialogContent>
      </Dialog>
    </>
  )
}

function Coils({
  scannerId,
  coils,
  canWrite,
}: {
  scannerId: string
  coils: Accessory[]
  canWrite: boolean
}) {
  const [editing, setEditing] = useState<Accessory | 'new' | null>(null)
  const remove = useDeleteAccessory(scannerId)

  async function onDelete(a: Accessory) {
    try {
      await remove.mutateAsync(a.id)
      toast.success(`Removed ${a.name}`)
    } catch (err) {
      toast.error(toApiError(err).message || 'Failed to remove')
    }
  }

  return (
    <DataCard
      title="Coils & accessories"
      actions={
        canWrite && (
          <Button size="sm" onClick={() => setEditing('new')}>
            <Plus className="h-3.5 w-3.5" />
            Add coil
          </Button>
        )
      }
    >
      {coils.length === 0 ? (
        <EmptyState text="No coils recorded." />
      ) : (
        <ul className="divide-y divide-border">
          {coils.map((a) => (
            <li key={a.id} className="flex items-center justify-between gap-2 px-5 py-3 text-sm">
              <div>
                <p className="font-medium">{a.name}</p>
                <p className="text-xs text-muted-foreground">
                  {[a.type, a.serialNumber].filter(Boolean).join(' · ') || '—'}
                </p>
              </div>
              {canWrite && (
                <div className="flex gap-1">
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label={`Edit ${a.name}`}
                    onClick={() => setEditing(a)}
                  >
                    <Pencil className="h-3.5 w-3.5" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="sm"
                    aria-label={`Remove ${a.name}`}
                    onClick={() => onDelete(a)}
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </Button>
                </div>
              )}
            </li>
          ))}
        </ul>
      )}
      <Dialog open={editing !== null} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editing === 'new' ? 'Add coil' : 'Edit coil'}</DialogTitle>
          </DialogHeader>
          {editing !== null && (
            <CoilForm
              scannerId={scannerId}
              coil={editing === 'new' ? undefined : editing}
              onDone={() => setEditing(null)}
            />
          )}
        </DialogContent>
      </Dialog>
    </DataCard>
  )
}

function CoilForm({
  scannerId,
  coil,
  onDone,
}: {
  scannerId: string
  coil?: Accessory
  onDone: () => void
}) {
  const save = useSaveAccessory(scannerId)
  const [f, setF] = useState({
    name: coil?.name ?? '',
    type: coil?.type ?? '',
    serialNumber: coil?.serialNumber ?? '',
  })
  const [error, setError] = useState<string | null>(null)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (!f.name.trim()) {
      setError('Name is required')
      return
    }
    try {
      await save.mutateAsync({ id: coil?.id, input: f })
      toast.success(coil ? 'Coil updated' : 'Coil added')
      onDone()
    } catch (err) {
      setError(toApiError(err).message)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-3">
      <Input
        label="Name"
        value={f.name}
        error={error ?? undefined}
        onChange={(e) => setF({ ...f, name: e.target.value })}
      />
      <Input label="Type" value={f.type} onChange={(e) => setF({ ...f, type: e.target.value })} />
      <Input
        label="Serial number"
        value={f.serialNumber}
        onChange={(e) => setF({ ...f, serialNumber: e.target.value })}
      />
      <div className="flex justify-end gap-2 pt-2">
        <Button type="button" variant="outline" onClick={onDone}>
          Cancel
        </Button>
        <Button type="submit" disabled={save.isPending}>
          Save
        </Button>
      </div>
    </form>
  )
}

function History({ scannerId }: { scannerId: string }) {
  const [page, setPage] = useState(0)
  const [open, setOpen] = useState<string | null>(null)
  const history = useCalibrationHistory(scannerId, page)

  return (
    <DataCard title="Calibration history">
      {history.isPending ? (
        <TableSkeleton />
      ) : history.isError ? (
        <div className="p-5">
          <ErrorState error={history.error} onRetry={() => history.refetch()} />
        </div>
      ) : history.data.content.length === 0 ? (
        <EmptyState text="No calibration sessions recorded yet." />
      ) : (
        <>
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-muted/40">
                <tr>
                  <th className={th}>Performed</th>
                  <th className={th}>Result</th>
                  <th className={th}>Next due</th>
                  <th className={th}>By</th>
                  <th className={th}>Certificate</th>
                  <th className={cn(th, 'text-right')}>Risk score (indicative)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {history.data.content.map((h) => (
                  <tr
                    key={h.id}
                    className="cursor-pointer transition-colors hover:bg-muted/30"
                    onClick={() => setOpen(h.id)}
                  >
                    <td className={td}>
                      <button className="font-medium hover:underline" onClick={() => setOpen(h.id)}>
                        {new Date(h.performedAt).toLocaleString()}
                      </button>
                    </td>
                    <td className={td}>
                      <ResultBadge result={h.result} />
                    </td>
                    <td className={td}>{formatDate(h.nextDueDate)}</td>
                    <td className={td}>{h.performedByName}</td>
                    <td className={td}>{h.certificateRef ?? '—'}</td>
                    <td className={cn(td, 'text-right')}>
                      <RiskScore value={h.riskScore} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          {history.data.totalPages > 1 && (
            <div className="flex items-center justify-end gap-3 border-t border-border px-5 py-3 text-sm text-muted-foreground">
              <Button
                variant="secondary"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                Previous
              </Button>
              <span>
                Page {page + 1} of {history.data.totalPages}
              </span>
              <Button
                variant="secondary"
                size="sm"
                disabled={page + 1 >= history.data.totalPages}
                onClick={() => setPage(page + 1)}
              >
                Next
              </Button>
            </div>
          )}
        </>
      )}
      <Dialog open={open !== null} onOpenChange={(o) => !o && setOpen(null)}>
        <DialogContent className="sm:max-w-2xl">
          <DialogHeader>
            <DialogTitle>Calibration session</DialogTitle>
          </DialogHeader>
          <SessionDetail sessionId={open} />
        </DialogContent>
      </Dialog>
    </DataCard>
  )
}

function SessionDetail({ sessionId }: { sessionId: string | null }) {
  const session = useCalibration(sessionId)
  if (session.isPending) return <TableSkeleton />
  if (session.isError) return <ErrorState error={session.error} />
  const d = session.data
  return (
    <div className="space-y-4 text-sm">
      <div className="flex flex-wrap items-center gap-x-6 gap-y-2">
        <span>
          Result: <ResultBadge result={d.result} />
        </span>
        <span>Performed: {new Date(d.performedAt).toLocaleString()}</span>
        <span>Next due: {formatDate(d.nextDueDate)}</span>
        <RiskScore value={d.riskScore} withLabel />
      </div>
      <p className="text-muted-foreground">
        By {d.performedByName}
        {d.certificateRef && ` · Certificate ${d.certificateRef}`}
        {d.notes && ` · ${d.notes}`}
      </p>
      <div className="overflow-x-auto rounded-lg border border-border">
        <table className="w-full">
          <thead className="bg-muted/40">
            <tr>
              <th className={th}>QA test</th>
              <th className={th}>Measured</th>
              <th className={th}>Tolerance at the time</th>
              <th className={th}>Within</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {d.measurements.map((m) => (
              <tr key={m.id}>
                <td className={td}>
                  {m.qaTestName}
                  <div className="font-mono text-xs text-muted-foreground">{m.qaTestCode}</div>
                </td>
                <td className={cn(td, 'tabular-nums')}>
                  {m.measuredValue} {m.unit ?? ''}
                </td>
                <td className={td}>{formatLimits(m.lowerLimit, m.upperLimit, m.unit)}</td>
                <td className={td}>
                  <ResultBadge result={m.withinTolerance ? 'PASS' : 'FAIL'} />
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="text-xs text-muted-foreground">
        Limits are shown as they were when the session was recorded. Some may be placeholders (
        {PLACEHOLDER_TEXT}).
      </p>
    </div>
  )
}
