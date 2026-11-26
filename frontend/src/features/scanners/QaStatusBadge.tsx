import type { CalibrationResult, QaStatus } from '@/api/types'
import { cn } from '@/lib/utils'
import { FLAGGED_TEXT, QA_STATUS_META, RESULT_TONE, TONE_CLASS, type Tone } from './qa'

function Pill({ tone, children }: { tone: Tone; children: React.ReactNode }) {
  return (
    <span
      data-tone={tone}
      className={cn(
        'inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-medium',
        TONE_CLASS[tone],
      )}
    >
      {children}
    </span>
  )
}

/** OK green, DUE_SOON amber, OVERDUE/FAILED red with "Flagged for review", NEVER_CALIBRATED grey. */
export function QaStatusBadge({ status }: { status: QaStatus }) {
  const meta = QA_STATUS_META[status]
  return (
    <Pill tone={meta.tone}>
      {meta.label}
      {meta.flagged && <span className="ml-1">· {FLAGGED_TEXT}</span>}
    </Pill>
  )
}

export function ResultBadge({ result }: { result: CalibrationResult | null }) {
  if (!result) return <span className="text-muted-foreground">—</span>
  return <Pill tone={RESULT_TONE[result]}>{result === 'PASS' ? 'Pass' : 'Fail'}</Pill>
}

/** Always labelled indicative: it helps prioritise review and is not a safety verdict. */
export function RiskScore({
  value,
  withLabel = false,
}: {
  value: number | null
  withLabel?: boolean
}) {
  const text = value === null ? '—' : value.toFixed(1)
  return withLabel ? (
    <span>
      Risk score (indicative): <span className="font-medium tabular-nums">{text}</span>
    </span>
  ) : (
    <span className="tabular-nums">{text}</span>
  )
}
