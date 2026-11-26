import { api } from '../api/client'
import type { LoginInput, LoginResult, Me } from '../api/types'

export interface RegisterPatientInput {
  name: string
  email: string
  password: string
  dateOfBirth: string
  phone?: string | null
  neurologyHistory?: string | null
}

export async function loginRequest(input: LoginInput): Promise<LoginResult> {
  const { data } = await api.post<LoginResult>('/api/auth/login', input)
  return data
}

export async function registerPatientRequest(input: RegisterPatientInput): Promise<LoginResult> {
  const { data } = await api.post<LoginResult>('/api/auth/patient/register', input)
  return data
}

export async function fetchMe(): Promise<Me> {
  const { data } = await api.get<Me>('/api/me')
  return data
}
