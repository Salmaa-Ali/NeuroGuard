import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { isAllowed, type Access } from '@/auth/access'
import { useAuth } from '@/auth/context'

/** Hides pages the user's role may not use. Convenience only: backend rules enforce access. */
export function RequireAccess({ access, children }: { access: Access; children: ReactNode }) {
  const { user } = useAuth()
  if (!isAllowed(user, access)) {
    return (
      <div className="mx-auto max-w-md py-20 text-center">
        <h1 className="text-xl font-semibold">Access denied</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Your role does not have access to this page.
        </p>
        <Link to="/" className="mt-4 inline-block text-sm font-medium text-primary hover:underline">
          Back to dashboard
        </Link>
      </div>
    )
  }
  return <>{children}</>
}
