import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Eye, Search, Stethoscope } from 'lucide-react'
import { PageHeader } from '@/components/app/PageHeader'
import { DataCard, EmptyState, TableSkeleton, td, th } from '@/components/app/DataTable'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { useAuth } from '@/auth/context'
import { toApiError } from '@/api/errors'
import { cn } from '@/lib/utils'
import { usePatients } from './api'

export function PatientsPage() {
  const { user } = useAuth()
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const size = 20

  const patientsQuery = usePatients(query, page, size)
  const rows = patientsQuery.data?.content ?? []
  const totalPages = Math.max(1, patientsQuery.data?.totalPages ?? 1)
  const isNeurologist = user?.scope === 'NEUROLOGIST'

  return (
    <>
      <PageHeader
        eyebrow="Patients"
        title="Patients"
        description="Browse and select patients by patient number or name."
      />

      <DataCard
        title="Patient directory"
        actions={
          <div className="relative w-64">
            <Search className="pointer-events-none absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Search patient # or name…"
              className="h-9 pl-8 text-sm"
              value={query}
              onChange={(e) => {
                setQuery(e.target.value)
                setPage(0)
              }}
            />
          </div>
        }
      >
        {patientsQuery.isLoading ? (
          <TableSkeleton />
        ) : patientsQuery.error ? (
          <EmptyState text={toApiError(patientsQuery.error).message || "Couldn't load patients."} />
        ) : rows.length === 0 ? (
          <EmptyState text="No patients found." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-muted/40">
                <tr>
                  <th className={th}>Patient #</th>
                  <th className={th}>Name</th>
                  <th className={th}>Email</th>
                  <th className={th}>Date of birth</th>
                  <th className={th}>Phone</th>
                  <th className={cn(th, 'text-right')}>Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {rows.map((p) => (
                  <tr key={p.id} className="transition-colors hover:bg-muted/30">
                    <td className={cn(td, 'font-mono text-xs font-semibold text-primary')}>
                      {p.patientNumber}
                    </td>
                    <td className={cn(td, 'font-medium')}>{p.name}</td>
                    <td className={cn(td, 'text-muted-foreground')}>{p.email}</td>
                    <td className={td}>{p.dateOfBirth}</td>
                    <td className={cn(td, 'text-muted-foreground')}>{p.phone || '—'}</td>
                    <td className={cn(td, 'text-right')}>
                      <div className="inline-flex items-center justify-end gap-1.5">
                        <Button asChild variant="ghost" size="sm">
                          <Link to={`/patients/${p.id}`}>
                            <Eye className="h-3.5 w-3.5" />
                            View
                          </Link>
                        </Button>
                        {isNeurologist && (
                          <Button asChild variant="outline" size="sm">
                            <Link to={`/clinical?patientId=${p.id}`}>
                              <Stethoscope className="h-3.5 w-3.5" />
                              Clinical
                            </Link>
                          </Button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </DataCard>

      {totalPages > 1 && (
        <div className="mt-3 flex items-center justify-end gap-3 text-sm text-muted-foreground">
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
      )}
    </>
  )
}
