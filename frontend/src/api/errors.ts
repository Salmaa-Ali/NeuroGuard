import axios from 'axios'
import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'

/** The error JSON every backend failure returns. (Not described in /v3/api-docs, so it is written by hand.) */
export interface ApiError {
  timestamp?: string
  /** HTTP status; 0 when the server could not be reached. */
  status: number
  error?: string
  message: string
  path?: string
  fieldErrors: { field: string; message: string }[]
}

export const NETWORK_ERROR_MESSAGE = 'Cannot reach the server. Check that the backend is running.'

function isApiErrorBody(data: unknown): data is Partial<ApiError> & { message: string } {
  return (
    typeof data === 'object' &&
    data !== null &&
    typeof (data as { message?: unknown }).message === 'string'
  )
}

/** Normalises anything thrown by axios (or elsewhere) into the backend's error shape. */
export function toApiError(err: unknown): ApiError {
  if (axios.isAxiosError(err)) {
    if (!err.response) return { status: 0, message: NETWORK_ERROR_MESSAGE, fieldErrors: [] }
    const { status, data } = err.response
    if (isApiErrorBody(data)) {
      return { ...data, status, message: data.message, fieldErrors: data.fieldErrors ?? [] }
    }
    return { status, message: `Request failed (${status})`, fieldErrors: [] }
  }
  return {
    status: 0,
    message: err instanceof Error ? err.message : 'Something went wrong',
    fieldErrors: [],
  }
}

export const isForbidden = (err: unknown) => toApiError(err).status === 403
export const isNotFound = (err: unknown) => toApiError(err).status === 404

/**
 * Puts the backend's fieldErrors onto the matching form fields.
 * Returns the messages that did not match any field, to show as a form-level error.
 */
export function applyFieldErrors<T extends FieldValues>(
  error: ApiError,
  setError: UseFormSetError<T>,
  formFields: readonly string[],
): string[] {
  const unmatched: string[] = []
  for (const fe of error.fieldErrors) {
    if (formFields.includes(fe.field)) {
      setError(fe.field as Path<T>, { type: 'server', message: fe.message })
    } else {
      unmatched.push(`${fe.field}: ${fe.message}`)
    }
  }
  if (error.fieldErrors.length === 0) unmatched.push(error.message)
  else if (unmatched.length > 0) unmatched.unshift(error.message)
  return unmatched
}
