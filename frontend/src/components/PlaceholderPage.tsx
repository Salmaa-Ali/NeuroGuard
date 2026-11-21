import { PageHeader } from './PageHeader'

interface PlaceholderPageProps {
  title: string
  /** One line about what will live here. */
  description: string
  phase: 2 | 3
}

/** Stand-in for features that are not built yet. Replace it inside the feature's own folder. */
export function PlaceholderPage({ title, description, phase }: PlaceholderPageProps) {
  return (
    <>
      <PageHeader
        title={title}
        actions={
          <span className="rounded-full bg-accent-50 px-3 py-1 text-xs font-medium text-accent-700 ring-1 ring-accent-100">
            Coming in Phase {phase}
          </span>
        }
      />
      <div className="rounded-lg border border-dashed border-slate-300 bg-white px-6 py-16 text-center">
        <p className="mx-auto max-w-lg text-sm text-slate-600">{description}</p>
      </div>
    </>
  )
}
