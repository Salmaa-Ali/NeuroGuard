import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
}

export type InvoiceStatus = 'UNPAID' | 'PAID' | 'CANCELLED'
export type PaymentStatus = 'COMPLETED' | 'FAILED'

export interface InvoiceItem {
  id: string
  serviceId: string
  serviceName: string
  icd10Code: string | null
  unitPrice: number
  quantity: number
  totalPrice: number
}

export interface PaymentRecord {
  id: string
  invoiceId: string
  patientId: string
  amountPaid: number
  status: PaymentStatus
  paymentDate: string
}

export interface InvoiceRecord {
  id: string
  patientId: string
  appointmentId: string | null
  status: InvoiceStatus
  issueDate: string
  totalAmount: number
  items?: InvoiceItem[]
  payment?: PaymentRecord | null
  createdAt?: string
  updatedAt?: string
}

export interface CreateInvoiceItemInput {
  serviceId: string
  quantity: number
  icd10Code?: string | null
}

export interface CreateInvoiceInput {
  patientId: string
  appointmentId?: string | null
  items: CreateInvoiceItemInput[]
}

export interface ServiceRecord {
  id: string
  name: string
  price: number
  createdAt: string
  updatedAt: string
}

export interface ServiceInput {
  name: string
  price: number
}

export async function fetchInvoices(
  status?: InvoiceStatus | 'ALL',
  patientId?: string,
  page = 0,
  size = 100,
): Promise<InvoiceRecord[]> {
  const params: Record<string, unknown> = { page, size }
  if (status && status !== 'ALL') {
    params.status = status
  }

  const url = patientId ? `/api/patients/${patientId}/invoices` : '/api/invoices'
  const res = await api.get<PageResponse<InvoiceRecord>>(url, { params })
  return res.data.content
}

export async function fetchInvoice(invoiceId: string): Promise<InvoiceRecord> {
  const res = await api.get<InvoiceRecord>(`/api/invoices/${invoiceId}`)
  return res.data
}

export async function createInvoice(input: CreateInvoiceInput): Promise<InvoiceRecord> {
  const res = await api.post<InvoiceRecord>('/api/invoices', input)
  return res.data
}

export async function fetchPayments(page = 0, size = 100): Promise<PaymentRecord[]> {
  const res = await api.get<PageResponse<PaymentRecord>>('/api/payments', {
    params: { page, size },
  })
  return res.data.content
}

export async function payInvoice(invoiceId: string): Promise<PaymentRecord> {
  const res = await api.post<PaymentRecord>(`/api/invoices/${invoiceId}/pay`)
  return res.data
}

export async function cancelInvoice(invoiceId: string): Promise<InvoiceRecord> {
  const res = await api.post<InvoiceRecord>(`/api/invoices/${invoiceId}/cancel`)
  return res.data
}

export async function fetchServices(page = 0, size = 100): Promise<ServiceRecord[]> {
  const res = await api.get<PageResponse<ServiceRecord>>('/api/services', {
    params: { page, size },
  })
  return res.data.content
}

export async function createService(input: ServiceInput): Promise<ServiceRecord> {
  const res = await api.post<ServiceRecord>('/api/services', input)
  return res.data
}

export async function updateService(id: string, input: ServiceInput): Promise<ServiceRecord> {
  const res = await api.put<ServiceRecord>(`/api/services/${id}`, input)
  return res.data
}

export function useInvoices(patientId?: string, status?: InvoiceStatus | 'ALL') {
  return useQuery({
    queryKey: ['invoices', patientId, status],
    queryFn: () => fetchInvoices(status, patientId),
  })
}

export function useInvoice(invoiceId?: string | null) {
  return useQuery({
    queryKey: ['invoice', invoiceId],
    queryFn: () => fetchInvoice(invoiceId!),
    enabled: Boolean(invoiceId),
  })
}

export function useCreateInvoice() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createInvoice,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['invoices'] })
    },
  })
}

export function usePayments() {
  return useQuery({
    queryKey: ['payments'],
    queryFn: () => fetchPayments(),
  })
}

export function usePayInvoice() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: payInvoice,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['invoices'] })
      void queryClient.invalidateQueries({ queryKey: ['invoice'] })
      void queryClient.invalidateQueries({ queryKey: ['payments'] })
    },
  })
}

export function useCancelInvoice() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: cancelInvoice,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['invoices'] })
      void queryClient.invalidateQueries({ queryKey: ['invoice'] })
    },
  })
}

export function useServices(enabled = true) {
  return useQuery({
    queryKey: ['services'],
    queryFn: () => fetchServices(),
    enabled,
  })
}

export function useCreateService() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createService,
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['services'] })
    },
  })
}

export function useUpdateService() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: ServiceInput }) => updateService(id, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['services'] })
    },
  })
}
