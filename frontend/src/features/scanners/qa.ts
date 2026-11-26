import type { CalibrationResult, QaStatus, ScannerStatus } from '@/api/types'

export type Tone = 'success' | 'warning' | 'danger' | 'neutral'

/**
 * How each QA status is shown. FAILED and OVERDUE are "Flagged for review": a prompt to review only,
 * never a statement that a scanner or any patient study is unsafe.
 */
export const QA_STATUS_META: Record<QaStatus, { label: string; tone: Tone; flagged: boolean }> = {
  OK: { label: 'OK', tone: 'success', flagged: false },
  DUE_SOON: { label: 'Due soon', tone: 'warning', flagged: false },
  OVERDUE: { label: 'Overdue', tone: 'danger', flagged: true },
  FAILED: { label: 'Failed', tone: 'danger', flagged: true },
  NEVER_CALIBRATED: { label: 'Never calibrated', tone: 'neutral', flagged: false },
}

export const FLAGGED_TEXT = 'Flagged for review'

export const RESULT_TONE: Record<CalibrationResult, Tone> = { PASS: 'success', FAIL: 'danger' }

export const SCANNER_STATUS_LABEL: Record<ScannerStatus, string> = {
  ACTIVE: 'Active',
  OUT_OF_SERVICE: 'Out of service',
  RETIRED: 'Retired',
}

export const TONE_CLASS: Record<Tone, string> = {
  success: 'border-success/30 bg-success/10 text-success',
  warning: 'border-warning-foreground/30 bg-warning text-warning-foreground',
  danger: 'border-destructive/30 bg-destructive/10 text-destructive',
  neutral: 'border-border bg-muted text-muted-foreground',
}

export const PLACEHOLDER_TEXT = 'Placeholder: confirm with medical physicist'

/** Same rule as the backend: inclusive bounds, a missing bound means no limit on that side. */
export function isWithin(value: number, lower: number | null, upper: number | null): boolean {
  if (lower !== null && value < lower) return false
  return upper === null || value <= upper
}

/** "188 – 192 mm", "≤ 2.5 %", "≥ 87.5 %" or "No limits". */
export function formatLimits(
  lower: number | null,
  upper: number | null,
  unit?: string | null,
): string {
  const u = unit ? ` ${unit}` : ''
  if (lower !== null && upper !== null) return `${lower} – ${upper}${u}`
  if (lower !== null) return `≥ ${lower}${u}`
  if (upper !== null) return `≤ ${upper}${u}`
  return 'No limits'
}

export const formatDate = (iso: string | null) =>
  iso
    ? new Date(iso.length === 10 ? `${iso}T00:00:00` : iso).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'short',
        day: 'numeric',
      })
    : '—'

/** datetime-local value ("2026-06-15T10:30") for a Date, in the browser's time zone. */
export function toLocalInput(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}
