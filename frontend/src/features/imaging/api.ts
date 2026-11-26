import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import type { PageResponse } from '../billing/api'
export type MriOrderStatus='ORDERED'|'SCHEDULED'|'ACQUIRED'|'REPORTED'|'CANCELLED'
export interface MriOrderRecord { id:string; appointmentId:string; patientId:string; patientName:string; accessionNumber:string; indication:string|null; status:MriOrderStatus }
export interface MriOrderInput { appointmentId:string; accessionNumber:string; indication?:string }
export async function fetchMriOrders(patientId?:string) { const url=patientId?`/api/patients/${patientId}/mri-orders`:'/api/mri-orders'; return (await api.get<PageResponse<MriOrderRecord>>(url)).data.content }
export async function createMriOrder(input:MriOrderInput) { return (await api.post<MriOrderRecord>('/api/mri-orders',input)).data }
export async function updateMriOrderStatus(id:string,status:MriOrderStatus) { return (await api.patch<MriOrderRecord>(`/api/mri-orders/${id}/status`,{status})).data }
export function useMriOrders(patientId?:string) { return useQuery({queryKey:['mri-orders',patientId],queryFn:()=>fetchMriOrders(patientId)}) }
export function useCreateMriOrder() { const qc=useQueryClient();return useMutation({mutationFn:createMriOrder,onSuccess:()=>void qc.invalidateQueries({queryKey:['mri-orders']})}) }
export function useMriOrderStatus() { const qc=useQueryClient();return useMutation({mutationFn:({id,status}:{id:string;status:MriOrderStatus})=>updateMriOrderStatus(id,status),onSuccess:()=>void qc.invalidateQueries({queryKey:['mri-orders']})}) }
