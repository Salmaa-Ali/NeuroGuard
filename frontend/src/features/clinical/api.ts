import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import type { PageResponse } from '@/features/billing/api'

export interface Icd10Record {
  code: string
  description: string
  category: string
}

export interface DiagnosisRecord {
  id: string
  patientId: string
  icd10Code: string
  icd10Description: string
  icd10Category: string
  diagnosedAt: string
  notes: string | null
  staffId: string
  staffName: string
  createdAt: string
}

export interface CreateDiagnosisInput {
  icd10Code: string
  diagnosedAt?: string | null
  notes?: string | null
}

export interface PrescriptionRecord {
  id: string
  planId: string
  medicationDetails: string
  staffId: string
  staffName: string
  createdAt: string
}

export interface TreatmentPlanRecord {
  id: string
  patientId: string
  description: string
  startDate: string
  endDate: string | null
  staffId: string
  staffName: string
  createdAt: string
  prescriptions: PrescriptionRecord[]
}

export interface CreateTreatmentPlanInput {
  description: string
  startDate: string
  endDate?: string | null
}

export interface CreatePrescriptionInput {
  medicationDetails: string
}

// ICD-10
export async function searchIcd10(
  q?: string,
  page = 0,
  size = 20,
): Promise<PageResponse<Icd10Record>> {
  const params: Record<string, unknown> = { page, size }
  if (q && q.trim()) params.q = q.trim()
  const { data } = await api.get<PageResponse<Icd10Record>>('/api/icd10', { params })
  return data
}

export async function getIcd10(code: string): Promise<Icd10Record> {
  const { data } = await api.get<Icd10Record>(`/api/icd10/${encodeURIComponent(code)}`)
  return data
}

export function useIcd10Search(q: string, page = 0, size = 20) {
  return useQuery({
    queryKey: ['icd10', q, page, size],
    queryFn: () => searchIcd10(q, page, size),
  })
}

// Diagnoses
export async function listPatientDiagnoses(
  patientId: string,
  page = 0,
  size = 50,
): Promise<DiagnosisRecord[]> {
  const { data } = await api.get<PageResponse<DiagnosisRecord>>(
    `/api/patients/${patientId}/diagnoses`,
    { params: { page, size } },
  )
  return data.content
}

export async function getDiagnosis(id: string): Promise<DiagnosisRecord> {
  const { data } = await api.get<DiagnosisRecord>(`/api/diagnoses/${id}`)
  return data
}

export async function createDiagnosis(
  patientId: string,
  input: CreateDiagnosisInput,
): Promise<DiagnosisRecord> {
  const { data } = await api.post<DiagnosisRecord>(
    `/api/patients/${patientId}/diagnoses`,
    input,
  )
  return data
}

export function usePatientDiagnoses(patientId?: string | null) {
  return useQuery({
    queryKey: ['diagnoses', patientId],
    queryFn: () => listPatientDiagnoses(patientId!),
    enabled: Boolean(patientId),
  })
}

export function useDiagnosis(id?: string | null) {
  return useQuery({
    queryKey: ['diagnosis', id],
    queryFn: () => getDiagnosis(id!),
    enabled: Boolean(id),
  })
}

export function useCreateDiagnosis(patientId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: CreateDiagnosisInput) => createDiagnosis(patientId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['diagnoses', patientId] })
    },
  })
}

// Treatment plans & prescriptions
export async function listPatientTreatmentPlans(
  patientId: string,
  page = 0,
  size = 50,
): Promise<TreatmentPlanRecord[]> {
  const { data } = await api.get<PageResponse<TreatmentPlanRecord>>(
    `/api/patients/${patientId}/treatment-plans`,
    { params: { page, size } },
  )
  return data.content
}

export async function createTreatmentPlan(
  patientId: string,
  input: CreateTreatmentPlanInput,
): Promise<TreatmentPlanRecord> {
  const { data } = await api.post<TreatmentPlanRecord>(
    `/api/patients/${patientId}/treatment-plans`,
    input,
  )
  return data
}

export async function listPlanPrescriptions(planId: string): Promise<PrescriptionRecord[]> {
  const { data } = await api.get<PrescriptionRecord[]>(
    `/api/treatment-plans/${planId}/prescriptions`,
  )
  return data
}

export async function addPrescription(
  planId: string,
  input: CreatePrescriptionInput,
): Promise<PrescriptionRecord> {
  const { data } = await api.post<PrescriptionRecord>(
    `/api/treatment-plans/${planId}/prescriptions`,
    input,
  )
  return data
}

export function usePatientTreatmentPlans(patientId?: string | null) {
  return useQuery({
    queryKey: ['treatment-plans', patientId],
    queryFn: () => listPatientTreatmentPlans(patientId!),
    enabled: Boolean(patientId),
  })
}

export function useCreateTreatmentPlan(patientId: string) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (input: CreateTreatmentPlanInput) => createTreatmentPlan(patientId, input),
    onSuccess: () => {
      void queryClient.invalidateQueries({ queryKey: ['treatment-plans', patientId] })
    },
  })
}

export function useAddPrescription(patientId?: string | null) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ planId, input }: { planId: string; input: CreatePrescriptionInput }) =>
      addPrescription(planId, input),
    onSuccess: () => {
      if (patientId) {
        void queryClient.invalidateQueries({ queryKey: ['treatment-plans', patientId] })
      } else {
        void queryClient.invalidateQueries({ queryKey: ['treatment-plans'] })
      }
    },
  })
}
