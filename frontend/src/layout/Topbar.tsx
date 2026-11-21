import { useAuth } from '../auth/context'
import { describeRole } from '../auth/labels'
import { Button } from '../components/Button'

export function Topbar() {
  const { user, isProfileLoading, logout } = useAuth()
  if (!user) return null

  return (
    <header className="flex items-center justify-between border-b border-slate-200 bg-white px-6 py-3">
      <span className="text-sm font-semibold text-accent-600 md:hidden">NeuroGuard</span>
      <div className="ml-auto flex items-center gap-4">
        <div className="text-right leading-tight">
          {isProfileLoading ? (
            <span
              role="status"
              aria-label="Loading your name"
              className="inline-block h-4 w-28 animate-pulse rounded bg-slate-200"
            />
          ) : (
            <p className="text-sm font-medium text-slate-900" title={user.email}>
              {user.name ?? user.email}
            </p>
          )}
          <p className="text-xs text-slate-500">{describeRole(user.role, user.scope)}</p>
        </div>
        <Button variant="secondary" onClick={logout}>
          Log out
        </Button>
      </div>
    </header>
  )
}
