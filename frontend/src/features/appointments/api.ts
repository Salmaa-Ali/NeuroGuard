import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import type { PageResponse } from '../billing/api'

export type AppointmentStatus = 'SCHEDULED' | 'COMPLETED' | 'CANCELLED' | 'MISSED'
export interface AppointmentRecord { id: string; patientId: string; patientName: string; staffId: string; staffName: string; scheduledTime: string; durationMinutes: number; status: AppointmentStatus; reason: string | null }
export interface AppointmentInput { patientId: string; staffId?: string; scheduledTime: string; durationMinutes: number; reason?: string }
export async function fetchAppointments(patientId?: string) {
  const url = patientId ? `/api/patients/${patientId}/appointments` : '/api/appointments'
  const { data } = await api.get<PageResponse<AppointmentRecord>>(url)
  return data.content
}
export async function createAppointment(input: AppointmentInput) { return (await api.post<AppointmentRecord>('/api/appointments', input)).data }
export async function updateAppointmentStatus(id: string, status: AppointmentStatus) { return (await api.patch<AppointmentRecord>(`/api/appointments/${id}/status`, { status })).data }
export function useAppointments(patientId?: string, enabled = true) { return useQuery({ queryKey: ['appointments', patientId], queryFn: () => fetchAppointments(patientId), enabled }) }
export function useCreateAppointment() { const qc=useQueryClient(); return useMutation({ mutationFn:createAppointment, onSuccess:()=>void qc.invalidateQueries({queryKey:['appointments']}) }) }
export function useAppointmentStatus() { const qc=useQueryClient(); return useMutation({ mutationFn:({id,status}:{id:string;status:AppointmentStatus})=>updateAppointmentStatus(id,status), onSuccess:()=>void qc.invalidateQueries({queryKey:['appointments']}) }) }
