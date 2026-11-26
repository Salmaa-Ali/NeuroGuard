import type { ReactNode } from 'react'
import { Skeleton } from '@/components/ui/skeleton'

export function DataCard({
  title,
  children,
  actions,
}: {
  title?: string
  children: ReactNode
  actions?: ReactNode
}) {
  return (
    <section className="overflow-hidden rounded-2xl border border-border bg-card shadow-card">
      {(title || actions) && (
        <div className="flex items-center justify-between gap-3 border-b border-border px-5 py-3.5">
          {title && <h2 className="text-sm font-semibold">{title}</h2>}
          {actions}
        </div>
      )}
      {children}
    </section>
  )
}

export function TableSkeleton() {
  return (
    <div className="space-y-2 p-5">
      {[0, 1, 2, 3].map((i) => (
        <Skeleton key={i} className="h-9 w-full" />
      ))}
    </div>
  )
}

export function EmptyState({ text }: { text: string }) {
  return <p className="px-5 py-14 text-center text-sm text-muted-foreground">{text}</p>
}

export const th =
  'px-5 py-3 text-left text-[11px] font-semibold uppercase tracking-wider text-muted-foreground'
export const td = 'px-5 py-3.5 text-sm'
