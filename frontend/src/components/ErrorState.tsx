import { isForbidden, toApiError } from '../api/errors'
import { AccessDenied } from './AccessDenied'
import { Button } from './Button'

interface ErrorStateProps {
  error: unknown
  onRetry?: () => void
}

/** Standard error view for data fetching. A 403 becomes the "no access" screen; the rest show the backend message. */
export function ErrorState({ error, onRetry }: ErrorStateProps) {
  if (isForbidden(error)) return <AccessDenied />
  const { message } = toApiError(error)
  return (
    <div role="alert" className="rounded-lg border border-red-200 bg-red-50 px-6 py-8 text-center">
      <p className="font-medium text-red-900">Something went wrong</p>
      <p className="mt-1 text-sm text-red-800">{message}</p>
      {onRetry && (
        <Button variant="secondary" className="mt-4" onClick={onRetry}>
          Try again
        </Button>
      )}
    </div>
  )
}
