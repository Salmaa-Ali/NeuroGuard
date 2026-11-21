import { useQuery } from '@tanstack/react-query'
import { api } from '../../api/client'
import type { PatientSummary } from '../../api/types'

export async function getPatient(patientId: string): Promise<PatientSummary> {
  const { data } = await api.get<PatientSummary>(`/api/patients/${patientId}`)
  return data
}

export function usePatient(patientId: string) {
  return useQuery({
    queryKey: ['patient', patientId],
    queryFn: () => getPatient(patientId),
    retry: false,
  })
}
