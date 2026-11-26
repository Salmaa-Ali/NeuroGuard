import { PageHeader } from '@/components/app/PageHeader'
import { DataCard, EmptyState, TableSkeleton, td, th } from '@/components/app/DataTable'
import { StatusBadge, day } from '@/components/app/StatusBadge'
import { useAuth } from '@/auth/context'
import { cn } from '@/lib/utils'
import { usePatientDiagnoses, usePatientTreatmentPlans } from './api'

export function MyPrescriptionsPage() {
  const { user } = useAuth()
  const plansQuery = usePatientTreatmentPlans(user?.id)
  const diagnosesQuery = usePatientDiagnoses(user?.id)

  const plans = plansQuery.data ?? []
  const prescriptions = plans.flatMap((p) =>
    p.prescriptions.map((rx) => ({
      ...rx,
      planDescription: p.description,
      startDate: p.startDate,
      endDate: p.endDate,
    })),
  )

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="My care"
        title="My prescriptions"
        description="The medications prescribed to you as part of your treatment plans."
      />

      <DataCard title="Prescribed medications">
        {plansQuery.isLoading ? (
          <TableSkeleton />
        ) : plansQuery.error ? (
          <EmptyState text="Couldn't load your prescriptions. Try again shortly." />
        ) : prescriptions.length === 0 ? (
          <EmptyState text="No prescriptions on record yet." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-muted/40">
                <tr>
                  <th className={th}>Medication</th>
                  <th className={th}>Treatment plan</th>
                  <th className={th}>Prescribed by</th>
                  <th className={th}>Date</th>
                  <th className={th}>Plan status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {prescriptions.map((rx) => (
                  <tr key={rx.id} className="transition-colors hover:bg-muted/30">
                    <td className={cn(td, 'font-medium')}>{rx.medicationDetails}</td>
                    <td className={cn(td, 'text-muted-foreground')}>{rx.planDescription}</td>
                    <td className={td}>Dr. {rx.staffName}</td>
                    <td className={td}>{day(rx.createdAt)}</td>
                    <td className={td}>
                      <StatusBadge status={rx.endDate ? 'COMPLETED' : 'ACTIVE'} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </DataCard>

      <DataCard title="My diagnoses">
        {diagnosesQuery.isLoading ? (
          <TableSkeleton />
        ) : diagnosesQuery.error ? (
          <EmptyState text="Couldn't load your diagnoses." />
        ) : (diagnosesQuery.data ?? []).length === 0 ? (
          <EmptyState text="No diagnoses recorded yet." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-muted/40">
                <tr>
                  <th className={th}>Code</th>
                  <th className={th}>Condition</th>
                  <th className={th}>Diagnosed by</th>
                  <th className={th}>Date</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {(diagnosesQuery.data ?? []).map((d) => (
                  <tr key={d.id} className="transition-colors hover:bg-muted/30">
                    <td className={cn(td, 'font-mono text-xs font-semibold text-primary')}>
                      {d.icd10Code}
                    </td>
                    <td className={td}>
                      <p className="font-medium">{d.icd10Description}</p>
                      {d.notes && (
                        <p className="mt-0.5 text-xs text-muted-foreground">{d.notes}</p>
                      )}
                    </td>
                    <td className={td}>Dr. {d.staffName}</td>
                    <td className={td}>{day(d.diagnosedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </DataCard>
    </div>
  )
}
