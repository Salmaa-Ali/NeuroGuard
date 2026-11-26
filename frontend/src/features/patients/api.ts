import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { PatientSummary } from '../../api/types'
import type { PageResponse } from '../billing/api'

export async function listPatients(
  q?: string,
  page = 0,
  size = 100,
): Promise<PageResponse<PatientSummary>> {
  const params: Record<string, unknown> = { page, size }
  if (q && q.trim()) params.q = q.trim()
  const { data } = await api.get<PageResponse<PatientSummary>>('/api/patients', { params })
  return data
}

export async function getPatient(patientId: string): Promise<PatientSummary> {
  const { data } = await api.get<PatientSummary>(`/api/patients/${patientId}`)
  return data
}

export async function getPatientByNumber(patientNumber: string): Promise<PatientSummary> {
  const { data } = await api.get<PatientSummary>(
    `/api/patients/by-number/${encodeURIComponent(patientNumber)}`,
  )
  return data
}

export function usePatients(q = '', page = 0, size = 100, enabled = true) {
  return useQuery({
    queryKey: ['patients', q, page, size],
    queryFn: () => listPatients(q, page, size),
    placeholderData: keepPreviousData,
    enabled,
  })
}

export function usePatient(patientId: string) {
  return useQuery({
    queryKey: ['patient', patientId],
    queryFn: () => getPatient(patientId),
    enabled: Boolean(patientId),
    retry: false,
  })
}
