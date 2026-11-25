import { useMemo, useState } from 'react'
import { toast } from 'sonner'
import { CreditCard, Eye, XCircle } from 'lucide-react'
import { DataCard, EmptyState, TableSkeleton, td, th } from './DataTable'
import { StatusBadge, day, money } from './StatusBadge'
import { cn } from '@/lib/utils'
import { toApiError } from '@/api/errors'
import { useAuth } from '@/auth/context'
import { Button } from '@/components/ui/button'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  useCancelInvoice,
  useInvoice,
  useInvoices,
  usePayInvoice,
  type InvoiceRecord,
  type InvoiceStatus,
} from '@/features/billing/api'

const FILTERS = ['ALL', 'UNPAID', 'PAID', 'CANCELLED'] as const

export function Stat({ label, value, tone }: { label: string; value: string; tone?: string }) {
  return (
    <div className="rounded-2xl border border-border bg-card p-5 shadow-card">
      <p className="text-xs font-medium uppercase tracking-wider text-muted-foreground">{label}</p>
      <p className={cn('mt-2 text-2xl font-semibold tabular-nums', tone)}>{value}</p>
    </div>
  )
}

/** Lists invoices visible to the caller (database rules decide which). */
export function InvoiceList({
  showPatient,
  patientId,
}: {
  showPatient: boolean
  patientId?: string
}) {
  const { user } = useAuth()
  const [filter, setFilter] = useState<(typeof FILTERS)[number]>('ALL')
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const { data, isLoading, error } = useInvoices(patientId)
  const payMutation = usePayInvoice()
  const cancelMutation = useCancelInvoice()

  const canCancel = user?.scope === 'BILLING_CLERK'
  const canPay = user?.scope === 'BILLING_CLERK' || user?.role === 'PATIENT'

  const rows = useMemo(
    () => (data ?? []).filter((r: InvoiceRecord) => filter === 'ALL' || r.status === filter),
    [data, filter],
  )

  const totals = useMemo(() => {
    const all = data ?? []
    const sum = (s: InvoiceStatus) =>
      all.filter((r) => r.status === s).reduce((a, r) => a + Number(r.totalAmount), 0)
    return { paid: sum('PAID'), unpaid: sum('UNPAID'), count: all.length }
  }, [data])

  async function handlePay(id: string) {
    try {
      await payMutation.mutateAsync(id)
      toast.success('Invoice paid successfully')
    } catch (err) {
      toast.error(toApiError(err).message || 'Failed to pay invoice')
    }
  }

  async function handleCancel(id: string) {
    try {
      await cancelMutation.mutateAsync(id)
      toast.success('Invoice cancelled')
    } catch (err) {
      toast.error(toApiError(err).message || 'Failed to cancel invoice')
    }
  }

  return (
    <div className="space-y-6">
      <div className="grid gap-4 sm:grid-cols-3">
        <Stat label="Invoices" value={String(totals.count)} />
        <Stat label="Paid" value={money(totals.paid)} tone="text-success" />
        <Stat label="Outstanding" value={money(totals.unpaid)} tone="text-warning-foreground" />
      </div>
      <DataCard
        title="Invoices"
        actions={
          <div className="flex gap-1 rounded-lg bg-muted p-1">
            {FILTERS.map((f) => (
              <button
                key={f}
                onClick={() => setFilter(f)}
                className={cn(
                  'rounded-md px-2.5 py-1 text-xs font-medium transition-colors cursor-pointer',
                  filter === f
                    ? 'bg-card text-foreground shadow-card'
                    : 'text-muted-foreground hover:text-foreground',
                )}
              >
                {f === 'ALL' ? 'All' : f.charAt(0) + f.slice(1).toLowerCase()}
              </button>
            ))}
          </div>
        }
      >
        {isLoading ? (
          <TableSkeleton />
        ) : error ? (
          <EmptyState text="Couldn't load invoices. Try again shortly." />
        ) : rows.length === 0 ? (
          <EmptyState text="No invoices here yet." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-muted/40">
                <tr>
                  <th className={th}>Invoice</th>
                  {showPatient && <th className={th}>Patient</th>}
                  <th className={th}>Issued</th>
                  <th className={th}>Status</th>
                  <th className={cn(th, 'text-right')}>Total</th>
                  <th className={cn(th, 'text-right')}>Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {rows.map((r: InvoiceRecord) => (
                  <tr key={r.id} className="transition-colors hover:bg-muted/30">
                    <td className={cn(td, 'font-mono text-xs text-muted-foreground')}>
                      {r.id.slice(0, 8)}
                    </td>
                    {showPatient && (
                      <td className={cn(td, 'font-mono text-xs')}>{r.patientId.slice(0, 8)}</td>
                    )}
                    <td className={td}>{day(r.issueDate)}</td>
                    <td className={td}>
                      <StatusBadge status={r.status} />
                    </td>
                    <td className={cn(td, 'text-right font-medium tabular-nums')}>
                      {money(Number(r.totalAmount))}
                    </td>
                    <td className={cn(td, 'text-right')}>
                      <div className="inline-flex items-center justify-end gap-1.5">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => setSelectedId(r.id)}
                          title="View invoice details"
                        >
                          <Eye className="h-3.5 w-3.5" />
                          View
                        </Button>
                        {r.status === 'UNPAID' && canPay && (
                          <Button
                            variant="default"
                            size="sm"
                            disabled={payMutation.isPending}
                            onClick={() => void handlePay(r.id)}
                          >
                            <CreditCard className="h-3.5 w-3.5" />
                            Pay
                          </Button>
                        )}
                        {r.status === 'UNPAID' && canCancel && (
                          <Button
                            variant="outline"
                            size="sm"
                            disabled={cancelMutation.isPending}
                            onClick={() => void handleCancel(r.id)}
                          >
                            <XCircle className="h-3.5 w-3.5" />
                            Cancel
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

      <InvoiceDetailDialog
        invoiceId={selectedId}
        onClose={() => setSelectedId(null)}
        canPay={canPay}
        canCancel={canCancel}
        onPay={(id) => void handlePay(id)}
        onCancel={(id) => void handleCancel(id)}
        isBusy={payMutation.isPending || cancelMutation.isPending}
      />
    </div>
  )
}

function InvoiceDetailDialog({
  invoiceId,
  onClose,
  canPay,
  canCancel,
  onPay,
  onCancel,
  isBusy,
}: {
  invoiceId: string | null
  onClose: () => void
  canPay: boolean
  canCancel: boolean
  onPay: (id: string) => void
  onCancel: (id: string) => void
  isBusy: boolean
}) {
  const { data: invoice, isLoading, error } = useInvoice(invoiceId)

  return (
    <Dialog open={invoiceId !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>Invoice details</DialogTitle>
          <DialogDescription>
            {invoice ? (
              <span className="font-mono text-xs">{invoice.id}</span>
            ) : (
              'Loading invoice breakdown…'
            )}
          </DialogDescription>
        </DialogHeader>

        {isLoading ? (
          <TableSkeleton />
        ) : error ? (
          <p className="text-sm text-destructive">{toApiError(error).message}</p>
        ) : invoice ? (
          <div className="space-y-5">
            <div className="grid gap-3 rounded-xl border border-border bg-muted/30 p-4 text-sm sm:grid-cols-2">
              <div>
                <p className="text-xs text-muted-foreground">Status</p>
                <div className="mt-1">
                  <StatusBadge status={invoice.status} />
                </div>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Issued</p>
                <p className="mt-1 font-medium">{day(invoice.issueDate)}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Patient ID</p>
                <p className="mt-1 font-mono text-xs">{invoice.patientId}</p>
              </div>
              <div>
                <p className="text-xs text-muted-foreground">Total amount</p>
                <p className="mt-1 text-base font-semibold tabular-nums">
                  {money(Number(invoice.totalAmount))}
                </p>
              </div>
            </div>

            <div className="overflow-hidden rounded-xl border border-border">
              <table className="w-full">
                <thead className="bg-muted/40">
                  <tr>
                    <th className={th}>Service</th>
                    <th className={th}>ICD-10</th>
                    <th className={cn(th, 'text-right')}>Unit price</th>
                    <th className={cn(th, 'text-right')}>Qty</th>
                    <th className={cn(th, 'text-right')}>Line total</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border">
                  {(invoice.items ?? []).map((item) => (
                    <tr key={item.id}>
                      <td className={td}>{item.serviceName}</td>
                      <td className={cn(td, 'font-mono text-xs text-muted-foreground')}>
                        {item.icd10Code ?? '—'}
                      </td>
                      <td className={cn(td, 'text-right tabular-nums')}>
                        {money(Number(item.unitPrice))}
                      </td>
                      <td className={cn(td, 'text-right tabular-nums')}>{item.quantity}</td>
                      <td className={cn(td, 'text-right font-medium tabular-nums')}>
                        {money(Number(item.totalPrice))}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {invoice.payment && (
              <div className="rounded-xl border border-success/30 bg-success/5 p-3.5 text-sm">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <span className="font-medium text-success">
                    Payment {invoice.payment.status.toLowerCase()}
                  </span>
                  <span className="text-xs text-muted-foreground">
                    {day(invoice.payment.paymentDate)} · {money(Number(invoice.payment.amountPaid))}
                  </span>
                </div>
              </div>
            )}

            {invoice.status === 'UNPAID' && (canPay || canCancel) && (
              <div className="flex justify-end gap-2 pt-1">
                {canCancel && (
                  <Button
                    variant="outline"
                    disabled={isBusy}
                    onClick={() => onCancel(invoice.id)}
                  >
                    Cancel invoice
                  </Button>
                )}
                {canPay && (
                  <Button disabled={isBusy} onClick={() => onPay(invoice.id)}>
                    Pay {money(Number(invoice.totalAmount))}
                  </Button>
                )}
              </div>
            )}
          </div>
        ) : null}
      </DialogContent>
    </Dialog>
  )
}
