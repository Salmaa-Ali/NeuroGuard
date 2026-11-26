import { PageHeader } from '@/components/app/PageHeader'
import { PaymentList } from '@/components/app/PaymentList'

export function PaymentsPage() {
  return (
    <>
      <PageHeader
        eyebrow="Billing"
        title="Payments"
        description="Received payments, newest first."
      />
      <PaymentList />
    </>
  )
}
