import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'sonner'
import { Plus } from 'lucide-react'
import { QA_STATUSES, SCANNER_STATUSES, type QaStatus, type ScannerStatus } from '@/api/types'
import { ACCESS, isAllowed } from '@/auth/access'
import { useAuth } from '@/auth/context'
import { DataCard, EmptyState, TableSkeleton, td, th } from '@/components/app/DataTable'
import { PageHeader } from '@/components/app/PageHeader'
import { ErrorState } from '@/components/ErrorState'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { cn } from '@/lib/utils'
import { useCreateScanner, useScanners } from './api'
import { QA_STATUS_META, SCANNER_STATUS_LABEL, formatDate } from './qa'
import { QaStatusBadge, ResultBadge, RiskScore } from './QaStatusBadge'
import { ScannerForm } from './ScannerForm'

const PAGE_SIZE = 20

/** Scanner list with QA status. Readers see everything; only the biomedical engineer gets write buttons. */
export function ScannersPage() {
  const { user } = useAuth()
  const canWrite = isAllowed(user, ACCESS.scannersWrite)
  const navigate = useNavigate()
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState<ScannerStatus | ''>('')
  const [qaStatus, setQaStatus] = useState<QaStatus | ''>('')
  const [adding, setAdding] = useState(false)
  const create = useCreateScanner()

  const list = useScanners({
    page,
    size: PAGE_SIZE,
    status: status || undefined,
    qaStatus: qaStatus || undefined,
  })

  const select = 'rounded-md border border-input bg-card px-2 py-1 text-sm'

  return (
    <>
      <PageHeader
        eyebrow="Imaging equipment"
        title="Scanners & QA"
        description="Scanner inventory and quality assurance. A flagged status is a prompt for review only; it never affects patient studies or reports."
        actions={
          canWrite && (
            <>
              <Button variant="outline" asChild>
                <Link to="/scanners/qa-tests">QA tests</Link>
              </Button>
              <Button onClick={() => setAdding(true)}>
                <Plus className="h-4 w-4" />
                Add scanner
              </Button>
            </>
          )
        }
      />

      <DataCard
        title="Scanners"
        actions={
          <div className="flex flex-wrap items-center gap-3 text-sm">
            <label className="flex items-center gap-2">
              Status
              <select
                aria-label="Filter by status"
                className={select}
                value={status}
                onChange={(e) => {
                  setStatus(e.target.value as ScannerStatus | '')
                  setPage(0)
                }}
              >
                <option value="">All</option>
                {SCANNER_STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {SCANNER_STATUS_LABEL[s]}
                  </option>
                ))}
              </select>
            </label>
            <label className="flex items-center gap-2">
              QA status
              <select
                aria-label="Filter by QA status"
                className={select}
                value={qaStatus}
                onChange={(e) => {
                  setQaStatus(e.target.value as QaStatus | '')
                  setPage(0)
                }}
              >
                <option value="">All</option>
                {QA_STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {QA_STATUS_META[s].label}
                  </option>
                ))}
              </select>
            </label>
          </div>
        }
      >
        {list.isPending ? (
          <TableSkeleton />
        ) : list.isError ? (
          <div className="p-5">
            <ErrorState error={list.error} onRetry={() => list.refetch()} />
          </div>
        ) : list.data.content.length === 0 ? (
          <EmptyState
            text={status || qaStatus ? 'No scanners match these filters.' : 'No scanners yet.'}
          />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-muted/40">
                <tr>
                  <th className={th}>Scanner</th>
                  <th className={th}>Field</th>
                  <th className={th}>Station</th>
                  <th className={th}>Status</th>
                  <th className={th}>QA status</th>
                  <th className={th}>Last QA</th>
                  <th className={th}>Next due</th>
                  <th className={cn(th, 'text-right')}>Risk score (indicative)</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {list.data.content.map((s) => (
                  <tr
                    key={s.id}
                    className="cursor-pointer transition-colors hover:bg-muted/30"
                    onClick={() => navigate(`/scanners/${s.id}`)}
                  >
                    <td className={td}>
                      <Link to={`/scanners/${s.id}`} className="font-medium hover:underline">
                        {s.name}
                      </Link>
                      <div className="text-xs text-muted-foreground">
                        {s.manufacturer} {s.model}
                      </div>
                    </td>
                    <td className={td}>{s.fieldStrength} T</td>
                    <td className={td}>{s.stationName ?? '—'}</td>
                    <td className={td}>{SCANNER_STATUS_LABEL[s.status]}</td>
                    <td className={td}>
                      <QaStatusBadge status={s.qaStatus} />
                    </td>
                    <td className={td}>
                      <div className="flex items-center gap-2">
                        {formatDate(s.lastQaAt)} <ResultBadge result={s.lastQaResult} />
                      </div>
                    </td>
                    <td className={td}>{formatDate(s.nextDueDate)}</td>
                    <td className={cn(td, 'text-right')}>
                      <RiskScore value={s.riskScore} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </DataCard>

      {list.data && list.data.totalPages > 1 && (
        <div className="mt-3 flex items-center justify-end gap-3 text-sm text-muted-foreground">
          <Button
            variant="secondary"
            size="sm"
            disabled={page === 0}
            onClick={() => setPage(page - 1)}
          >
            Previous
          </Button>
          <span>
            Page {page + 1} of {list.data.totalPages}
          </span>
          <Button
            variant="secondary"
            size="sm"
            disabled={page + 1 >= list.data.totalPages}
            onClick={() => setPage(page + 1)}
          >
            Next
          </Button>
        </div>
      )}

      <Dialog open={adding} onOpenChange={setAdding}>
        <DialogContent className="sm:max-w-xl">
          <DialogHeader>
            <DialogTitle>Add scanner</DialogTitle>
          </DialogHeader>
          <ScannerForm
            submitLabel="Add scanner"
            onCancel={() => setAdding(false)}
            onSubmit={async (input) => {
              const created = await create.mutateAsync(input)
              toast.success(`Added ${created.name}`)
              setAdding(false)
              navigate(`/scanners/${created.id}`)
            }}
          />
        </DialogContent>
      </Dialog>
    </>
  )
}
