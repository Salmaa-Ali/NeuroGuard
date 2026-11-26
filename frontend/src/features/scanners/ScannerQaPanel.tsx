import { ErrorState } from '@/components/ErrorState'
import { Skeleton } from '@/components/ui/skeleton'
import { useQaSummary } from './api'
import { FLAGGED_TEXT, QA_STATUS_META, formatDate } from './qa'
import { QaStatusBadge, ResultBadge, RiskScore } from './QaStatusBadge'

/**
 * Read-only QA panel for one scanner (calls GET /api/scanners/{id}/qa-summary).
 * Exported for the Phase 3 DICOM viewer's equipment panel. A flagged status is a prompt for review only:
 * it never blocks, invalidates or changes any study or report.
 */
export function ScannerQaPanel({ scannerId }: { scannerId: string }) {
  const summary = useQaSummary(scannerId)

  return (
    <section
      aria-label="Scanner QA status"
      className="rounded-2xl border border-border bg-card p-5 shadow-card"
    >
      <h2 className="text-sm font-semibold">Scanner QA</h2>
      {summary.isPending ? (
        <div className="mt-3 space-y-2">
          <Skeleton className="h-5 w-40" />
          <Skeleton className="h-5 w-56" />
        </div>
      ) : summary.isError ? (
        <div className="mt-3">
          <ErrorState error={summary.error} onRetry={() => summary.refetch()} />
        </div>
      ) : (
        <dl className="mt-3 grid grid-cols-[auto_1fr] gap-x-4 gap-y-2 text-sm">
          <dt className="text-muted-foreground">Scanner</dt>
          <dd className="font-medium">
            {summary.data.scannerName} · {summary.data.fieldStrength} T
          </dd>
          <dt className="text-muted-foreground">QA status</dt>
          <dd>
            <QaStatusBadge status={summary.data.qaStatus} />
          </dd>
          <dt className="text-muted-foreground">Last QA</dt>
          <dd className="flex items-center gap-2">
            {formatDate(summary.data.lastQaAt)} <ResultBadge result={summary.data.lastQaResult} />
          </dd>
          <dt className="text-muted-foreground">Next due</dt>
          <dd>{formatDate(summary.data.nextDueDate)}</dd>
          <dt className="text-muted-foreground">Risk score (indicative)</dt>
          <dd>
            <RiskScore value={summary.data.riskScore} />
          </dd>
        </dl>
      )}
      {summary.data && QA_STATUS_META[summary.data.qaStatus].flagged && (
        <p className="mt-3 text-xs text-muted-foreground">
          {FLAGGED_TEXT}: QA needs attention. This does not affect any study or report.
        </p>
      )}
    </section>
  )
}
