import { DataCard, EmptyState, TableSkeleton, td, th } from './DataTable'
import { Stat } from './InvoiceList'
import { StatusBadge, day, money } from './StatusBadge'
import { cn } from '@/lib/utils'
import { usePayments, type PaymentRecord } from '@/features/billing/api'

export function PaymentList() {
  const { data, isLoading, error } = usePayments()

  const total = (data ?? []).reduce((a, r: PaymentRecord) => a + Number(r.amountPaid), 0)

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-2">
        <Stat label="Payments" value={String(data?.length ?? 0)} />
        <Stat label="Collected" value={money(total)} tone="text-success" />
      </div>
      <DataCard title="Payment history">
        {isLoading ? (
          <TableSkeleton />
        ) : error ? (
          <EmptyState text="Couldn't load payments. Try again shortly." />
        ) : !data?.length ? (
          <EmptyState text="No payments yet." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-muted/40">
                <tr>
                  <th className={th}>Payment</th>
                  <th className={th}>Invoice</th>
                  <th className={th}>Date</th>
                  <th className={th}>Status</th>
                  <th className={cn(th, 'text-right')}>Amount</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {data.map((r: PaymentRecord) => (
                  <tr key={r.id} className="transition-colors hover:bg-muted/30">
                    <td className={cn(td, 'font-mono text-xs text-muted-foreground')}>
                      {r.id.slice(0, 8)}
                    </td>
                    <td className={cn(td, 'font-mono text-xs')}>{r.invoiceId.slice(0, 8)}</td>
                    <td className={td}>{day(r.paymentDate)}</td>
                    <td className={td}>
                      <StatusBadge status={r.status} />
                    </td>
                    <td className={cn(td, 'text-right font-medium tabular-nums')}>
                      {money(Number(r.amountPaid))}
                    </td>
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
