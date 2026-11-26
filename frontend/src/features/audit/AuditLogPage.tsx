import { useState } from 'react'
import { PageHeader } from '@/components/app/PageHeader'
import { DataCard, EmptyState, TableSkeleton, td, th } from '@/components/app/DataTable'
import { StatusBadge } from '@/components/app/StatusBadge'
import { Button } from '@/components/ui/button'
import { toApiError } from '@/api/errors'
import { cn } from '@/lib/utils'
import { useAuditLogs, type ActorType } from './api'

const ACTOR_FILTERS = ['ALL', 'STAFF', 'PATIENT'] as const

const ACTION_OPTIONS = [
  'ALL',
  'LOGIN_SUCCESS',
  'LOGIN_FAILED',
  'PATIENT_REGISTERED',
  'STAFF_CREATED',
  'STAFF_ROLE_CHANGED',
  'DIAGNOSIS_CREATED',
  'TREATMENT_PLAN_CREATED',
  'PRESCRIPTION_CREATED',
  'SERVICE_CREATED',
  'SERVICE_UPDATED',
  'INVOICE_CREATED',
  'INVOICE_CANCELLED',
  'PAYMENT_COMPLETED',
] as const

function formatTimestamp(iso: string) {
  return new Date(iso).toLocaleString(undefined, {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  })
}

export function AuditLogPage() {
  const [actorType, setActorType] = useState<(typeof ACTOR_FILTERS)[number]>('ALL')
  const [action, setAction] = useState<string>('ALL')
  const [page, setPage] = useState(0)
  const size = 20

  const logsQuery = useAuditLogs({
    actorType: actorType as ActorType | 'ALL',
    action,
    page,
    size,
  })

  const rows = logsQuery.data?.content ?? []
  const totalPages = Math.max(1, logsQuery.data?.totalPages ?? 1)
  const totalElements = logsQuery.data?.totalElements ?? 0

  return (
    <>
      <PageHeader
        eyebrow="Administration"
        title="Audit log"
        description="Chronological trail of logins, registrations, staff changes, clinical writes, and billing events."
      />

      <DataCard
        title="Audit events"
        actions={
          <div className="flex flex-wrap items-center gap-2">
            <div className="flex gap-1 rounded-lg bg-muted p-1">
              {ACTOR_FILTERS.map((f) => (
                <button
                  key={f}
                  type="button"
                  onClick={() => {
                    setActorType(f)
                    setPage(0)
                  }}
                  className={cn(
                    'rounded-md px-2.5 py-1 text-xs font-medium transition-colors cursor-pointer',
                    actorType === f
                      ? 'bg-card text-foreground shadow-card'
                      : 'text-muted-foreground hover:text-foreground',
                  )}
                >
                  {f === 'ALL' ? 'All actors' : f.charAt(0) + f.slice(1).toLowerCase()}
                </button>
              ))}
            </div>
            <select
              aria-label="Filter by action"
              value={action}
              onChange={(e) => {
                setAction(e.target.value)
                setPage(0)
              }}
              className="h-8 rounded-md border border-input bg-card px-2.5 text-xs"
            >
              {ACTION_OPTIONS.map((a) => (
                <option key={a} value={a}>
                  {a === 'ALL' ? 'All actions' : a}
                </option>
              ))}
            </select>
          </div>
        }
      >
        {logsQuery.isLoading ? (
          <TableSkeleton />
        ) : logsQuery.error ? (
          <EmptyState text={toApiError(logsQuery.error).message || "Couldn't load audit logs."} />
        ) : rows.length === 0 ? (
          <EmptyState text="No audit events match the selected filters." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-muted/40">
                <tr>
                  <th className={th}>Timestamp</th>
                  <th className={th}>Actor</th>
                  <th className={th}>Action</th>
                  <th className={th}>Actor ID</th>
                  <th className={th}>Entity</th>
                  <th className={th}>Entity ID</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {rows.map((r) => (
                  <tr key={r.id} className="transition-colors hover:bg-muted/30">
                    <td className={cn(td, 'whitespace-nowrap text-xs text-muted-foreground')}>
                      {formatTimestamp(r.createdAt)}
                    </td>
                    <td className={td}>
                      <StatusBadge status={r.actorType} />
                    </td>
                    <td className={cn(td, 'font-mono text-xs font-medium')}>{r.action}</td>
                    <td className={cn(td, 'font-mono text-xs text-muted-foreground')}>
                      {r.actorId ? r.actorId.slice(0, 8) : '—'}
                    </td>
                    <td className={cn(td, 'text-xs')}>{r.entityType || '—'}</td>
                    <td className={cn(td, 'font-mono text-xs text-muted-foreground')}>
                      {r.entityId ? r.entityId.slice(0, 8) : '—'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </DataCard>

      {totalElements > 0 && (
        <div className="mt-3 flex flex-wrap items-center justify-between gap-3 text-sm text-muted-foreground">
          <span>
            Showing {page * size + 1}–{page * size + rows.length} of {totalElements}
          </span>
          <div className="flex items-center gap-3">
            <Button
              variant="secondary"
              size="sm"
              disabled={page === 0}
              onClick={() => setPage((p) => p - 1)}
            >
              Previous
            </Button>
            <span>
              Page {page + 1} of {totalPages}
            </span>
            <Button
              variant="secondary"
              size="sm"
              disabled={page + 1 >= totalPages}
              onClick={() => setPage((p) => p + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      )}
    </>
  )
}
