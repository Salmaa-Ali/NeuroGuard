import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../../api/client'
import type {
  ChangeRoleInput,
  CreateStaffInput,
  StaffMember,
  StaffPage,
  StaffSortBy,
} from '../../api/types'

export interface StaffQuery {
  page: number
  size: number
  sortBy: StaffSortBy
}

export async function listStaff(query: StaffQuery): Promise<StaffPage> {
  const { data } = await api.get<StaffPage>('/api/admin/staff', { params: query })
  return data
}

export async function createStaff(input: CreateStaffInput): Promise<StaffMember> {
  const { data } = await api.post<StaffMember>('/api/admin/staff', input)
  return data
}

export async function changeStaffRole(id: string, input: ChangeRoleInput): Promise<StaffMember> {
  const { data } = await api.patch<StaffMember>(`/api/admin/staff/${id}/role`, input)
  return data
}

export function useStaffList(query: StaffQuery) {
  return useQuery({
    queryKey: ['staff', query],
    queryFn: () => listStaff(query),
    placeholderData: keepPreviousData,
    retry: false,
  })
}

export function useCreateStaff() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createStaff,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['staff'] }),
  })
}

export function useChangeStaffRole() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: ChangeRoleInput }) =>
      changeStaffRole(id, input),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['staff'] }),
  })
}
