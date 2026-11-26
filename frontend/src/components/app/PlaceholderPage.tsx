import { Sparkles } from 'lucide-react'
import { PageHeader } from './PageHeader'

/** Stand-in for features the backend does not offer yet. */
export function PlaceholderPage({
  title,
  description,
  phase = 2,
}: {
  title: string
  description: string
  phase?: 2 | 3
}) {
  return (
    <>
      <PageHeader title={title} />
      <div className="relative overflow-hidden rounded-2xl border border-border bg-card px-6 py-16 text-center shadow-card">
        <div className="absolute inset-0 bg-grid opacity-60 [mask-image:radial-gradient(ellipse_at_center,black,transparent_70%)]" />
        <div className="relative">
          <span className="mx-auto grid h-12 w-12 place-items-center rounded-2xl bg-accent text-accent-foreground ring-1 ring-primary/25">
            <Sparkles className="h-5 w-5" />
          </span>
          <span className="mt-4 inline-block rounded-full border border-primary/30 bg-primary/10 px-3 py-1 text-xs font-medium text-primary">
            Coming in Phase {phase}
          </span>
          <p className="mx-auto mt-3 max-w-lg text-sm text-muted-foreground">{description}</p>
        </div>
      </div>
    </>
  )
}
