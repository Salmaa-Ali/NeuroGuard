import { cn } from '@/lib/utils'

const TONE: Record<string, string> = {
  PAID: 'border-success/30 bg-success/10 text-success',
  COMPLETED: 'border-success/30 bg-success/10 text-success',
  UNPAID: 'border-warning-foreground/30 bg-warning text-warning-foreground',
  CANCELLED: 'border-border bg-muted text-muted-foreground',
  FAILED: 'border-destructive/30 bg-destructive/10 text-destructive',
}

export function StatusBadge({ status }: { status: string }) {
  return (
    <span
      className={cn(
        'inline-flex rounded-full border px-2.5 py-0.5 text-xs font-medium',
        TONE[status] ?? 'border-primary/30 bg-primary/10 text-primary',
      )}
    >
      {status.charAt(0) + status.slice(1).toLowerCase()}
    </span>
  )
}

export const money = (n: number) =>
  new Intl.NumberFormat(undefined, { style: 'currency', currency: 'EGP' }).format(n)

export const day = (d: string) =>
  new Date(d).toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' })
