import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { api } from '@/api/client'
import type { PageResponse } from '@/features/billing/api'

export type ActorType = 'STAFF' | 'PATIENT'

export interface AuditLogRecord {
  id: string
  actorId: string | null
  actorType: ActorType
  action: string
  entityType: string | null
  entityId: string | null
  createdAt: string
}

export interface AuditLogQuery {
  action?: string
  actorType?: ActorType | 'ALL'
  page: number
  size: number
}

export async function fetchAuditLogs(query: AuditLogQuery): Promise<PageResponse<AuditLogRecord>> {
  const params: Record<string, unknown> = {
    page: query.page,
    size: query.size,
  }
  if (query.action && query.action !== 'ALL') {
    params.action = query.action
  }
  if (query.actorType && query.actorType !== 'ALL') {
    params.actorType = query.actorType
  }
  const { data } = await api.get<PageResponse<AuditLogRecord>>('/api/audit-logs', { params })
  return data
}

export function useAuditLogs(query: AuditLogQuery) {
  return useQuery({
    queryKey: ['audit-logs', query],
    queryFn: () => fetchAuditLogs(query),
    placeholderData: keepPreviousData,
  })
}
