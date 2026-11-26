import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '@/api/client'
import type {
  Accessory,
  AccessoryInput,
  CalibrationDetail,
  CalibrationPage,
  QaOverview,
  QaStatus,
  QaSummary,
  QaTest,
  QaTestInput,
  RecordCalibrationInput,
  ScannerDetail,
  ScannerInput,
  ScannerPage,
  ScannerStatus,
} from '@/api/types'

export interface ScannerQuery {
  page: number
  size: number
  status?: ScannerStatus
  qaStatus?: QaStatus
}

const KEY = ['scanners'] as const

// ---- reads ----

export function useScanners(query: ScannerQuery) {
  return useQuery({
    queryKey: [...KEY, 'list', query],
    queryFn: async () => (await api.get<ScannerPage>('/api/scanners', { params: query })).data,
    placeholderData: keepPreviousData,
    retry: false,
  })
}

export function useScanner(id: string) {
  return useQuery({
    queryKey: [...KEY, 'detail', id],
    queryFn: async () => (await api.get<ScannerDetail>(`/api/scanners/${id}`)).data,
    retry: false,
  })
}

export function useCalibrationHistory(scannerId: string, page: number, size = 20) {
  return useQuery({
    queryKey: [...KEY, 'history', scannerId, page, size],
    queryFn: async () =>
      (
        await api.get<CalibrationPage>(`/api/scanners/${scannerId}/calibrations`, {
          params: { page, size },
        })
      ).data,
    placeholderData: keepPreviousData,
    retry: false,
  })
}

export function useCalibration(sessionId: string | null) {
  return useQuery({
    queryKey: [...KEY, 'session', sessionId],
    queryFn: async () => (await api.get<CalibrationDetail>(`/api/calibrations/${sessionId}`)).data,
    enabled: sessionId !== null,
    retry: false,
  })
}

/** Compact read-only status used by <ScannerQaPanel> (and the Phase 3 viewer). */
export function useQaSummary(scannerId: string) {
  return useQuery({
    queryKey: [...KEY, 'qa-summary', scannerId],
    queryFn: async () => (await api.get<QaSummary>(`/api/scanners/${scannerId}/qa-summary`)).data,
    retry: false,
  })
}

export function useQaOverview() {
  return useQuery({
    queryKey: [...KEY, 'overview'],
    queryFn: async () => (await api.get<QaOverview>('/api/scanners/qa-overview')).data,
    retry: false,
  })
}

export function useQaTests(activeOnly = false) {
  return useQuery({
    queryKey: ['qa-tests', activeOnly],
    queryFn: async () =>
      (await api.get<QaTest[]>('/api/qa-tests', { params: { activeOnly } })).data,
    retry: false,
  })
}

// ---- writes (biomedical engineer) ----

function useInvalidate() {
  const queryClient = useQueryClient()
  return () => queryClient.invalidateQueries({ queryKey: KEY })
}

export function useCreateScanner() {
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: async (input: ScannerInput) =>
      (await api.post<ScannerDetail>('/api/scanners', input)).data,
    onSuccess: invalidate,
  })
}

export function useUpdateScanner(id: string) {
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: async (input: ScannerInput) =>
      (await api.put<ScannerDetail>(`/api/scanners/${id}`, input)).data,
    onSuccess: invalidate,
  })
}

export function useChangeScannerStatus(id: string) {
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: async (status: ScannerStatus) =>
      (await api.patch<ScannerDetail>(`/api/scanners/${id}/status`, { status })).data,
    onSuccess: invalidate,
  })
}

export function useSaveAccessory(scannerId: string) {
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: async ({ id, input }: { id?: string; input: AccessoryInput }) =>
      id
        ? (await api.put<Accessory>(`/api/scanners/${scannerId}/accessories/${id}`, input)).data
        : (await api.post<Accessory>(`/api/scanners/${scannerId}/accessories`, input)).data,
    onSuccess: invalidate,
  })
}

export function useDeleteAccessory(scannerId: string) {
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: async (id: string) => {
      await api.delete(`/api/scanners/${scannerId}/accessories/${id}`)
    },
    onSuccess: invalidate,
  })
}

export function useRecordCalibration(scannerId: string) {
  const invalidate = useInvalidate()
  return useMutation({
    mutationFn: async (input: RecordCalibrationInput) =>
      (await api.post<CalibrationDetail>(`/api/scanners/${scannerId}/calibrations`, input)).data,
    onSuccess: invalidate,
  })
}

export function useSaveQaTest() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: async ({ id, input }: { id?: string; input: QaTestInput }) =>
      id
        ? (await api.put<QaTest>(`/api/qa-tests/${id}`, input)).data
        : (await api.post<QaTest>('/api/qa-tests', input)).data,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['qa-tests'] }),
  })
}
