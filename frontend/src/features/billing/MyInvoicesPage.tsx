import { PageHeader } from '@/components/app/PageHeader'
import { InvoiceList } from '@/components/app/InvoiceList'
import { useAuth } from '@/auth/context'

export function MyInvoicesPage() {
  const { user } = useAuth()

  return (
    <>
      <PageHeader
        eyebrow="My care"
        title="My invoices"
        description="Your invoices and what is still outstanding."
      />
      <InvoiceList showPatient={false} patientId={user?.id} />
    </>
  )
}
