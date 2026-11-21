import { api } from '../api/client'
import type { LoginInput, LoginResult, Me } from '../api/types'

export type LoginKind = 'staff' | 'patient'

export async function loginRequest(kind: LoginKind, input: LoginInput): Promise<LoginResult> {
  const { data } = await api.post<LoginResult>(`/api/auth/${kind}/login`, input)
  return data
}

export async function fetchMe(): Promise<Me> {
  const { data } = await api.get<Me>('/api/me')
  return data
}
