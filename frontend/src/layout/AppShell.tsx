import { useState } from 'react'
import { Link, NavLink, Outlet, useLocation } from 'react-router-dom'
import { Brain, LogOut, Menu } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Sheet, SheetContent, SheetTitle } from '@/components/ui/sheet'
import { useAuth } from '@/auth/context'
import type { AuthUser } from '@/auth/context'
import { getAreas } from '@/auth/access'
import { describeRole } from '@/auth/labels'
import { FallbackIcon, NAV_ICON, groupAreas } from '@/components/app/nav-meta'
import { cn } from '@/lib/utils'

function initials(s: string) {
  return s
    .split(/[\s@.]+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((p) => p[0]?.toUpperCase())
    .join('')
}

function SidebarNav({ user, onNavigate }: { user: AuthUser; onNavigate?: () => void }) {
  const groups = groupAreas(getAreas(user))
  const DashIcon = NAV_ICON['/'] ?? FallbackIcon
  const linkCls =
    'group flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium transition-all hover:bg-sidebar-accent hover:text-sidebar-accent-foreground'

  return (
    <div className="flex h-full flex-col">
      <Link to="/" onClick={onNavigate} className="flex items-center gap-2.5 px-5 py-5">
        <span className="relative grid h-9 w-9 place-items-center rounded-xl bg-gradient-primary text-primary-foreground shadow-glow">
          <Brain className="h-5 w-5" />
        </span>
        <span className="text-lg font-semibold tracking-tight text-sidebar-accent-foreground">
          NeuroGuard
        </span>
      </Link>
      <nav aria-label="Main" className="flex-1 space-y-5 overflow-y-auto px-3 pb-6">
        <ul>
          <li>
            <NavLink
              to="/"
              end
              onClick={onNavigate}
              className={({ isActive }) =>
                cn(
                  linkCls,
                  isActive &&
                    'bg-sidebar-accent text-sidebar-accent-foreground shadow-[inset_2px_0_0_var(--sidebar-primary)]',
                )
              }
            >
              {({ isActive }) => (
                <>
                  <DashIcon
                    className={cn(
                      'h-4 w-4 opacity-70',
                      isActive ? 'text-sidebar-primary opacity-100' : '',
                    )}
                  />
                  Dashboard
                </>
              )}
            </NavLink>
          </li>
        </ul>
        {groups.map(([group, items]) => (
          <div key={group}>
            <p className="mb-1.5 px-3 text-[11px] font-semibold uppercase tracking-[0.12em] text-sidebar-foreground/50">
              {group}
            </p>
            <ul className="space-y-0.5">
              {items.map((item) => {
                const Icon = NAV_ICON[item.to] ?? FallbackIcon
                return (
                  <li key={item.to}>
                    <NavLink
                      to={item.to}
                      onClick={onNavigate}
                      className={({ isActive }) =>
                        cn(
                          linkCls,
                          isActive &&
                            'bg-sidebar-accent text-sidebar-accent-foreground shadow-[inset_2px_0_0_var(--sidebar-primary)]',
                        )
                      }
                    >
                      {({ isActive }) => (
                        <>
                          <Icon
                            className={cn(
                              'h-4 w-4 opacity-70',
                              isActive ? 'text-sidebar-primary opacity-100' : '',
                            )}
                          />
                          {item.label}
                        </>
                      )}
                    </NavLink>
                  </li>
                )
              })}
            </ul>
          </div>
        ))}
      </nav>
      <div className="border-t border-sidebar-border px-5 py-4 text-xs text-sidebar-foreground/60">
        <span className="inline-flex items-center gap-2">
          <span className="relative flex h-2 w-2">
            <span className="absolute inline-flex h-full w-full animate-pulse-ring rounded-full bg-success" />
            <span className="relative inline-flex h-2 w-2 rounded-full bg-success" />
          </span>
          Secure session
        </span>
      </div>
    </div>
  )
}

export function AppShell() {
  const { user, logout } = useAuth()
  const [open, setOpen] = useState(false)
  const location = useLocation()

  if (!user) return null

  return (
    <div className="flex min-h-screen bg-background">
      <aside className="sticky top-0 hidden h-screen w-64 shrink-0 border-r border-sidebar-border bg-sidebar text-sidebar-foreground md:block">
        <SidebarNav user={user} />
      </aside>
      <Sheet open={open} onOpenChange={setOpen}>
        <SheetContent
          side="left"
          className="w-72 border-sidebar-border bg-sidebar p-0 text-sidebar-foreground"
        >
          <SheetTitle className="sr-only">Navigation</SheetTitle>
          <SidebarNav user={user} onNavigate={() => setOpen(false)} />
        </SheetContent>
      </Sheet>
      <div className="flex min-w-0 flex-1 flex-col">
        <header className="sticky top-0 z-20 flex items-center gap-3 border-b border-border bg-background/75 px-4 py-3 backdrop-blur-md sm:px-6">
          <Button
            variant="ghost"
            size="icon"
            className="md:hidden"
            onClick={() => setOpen(true)}
            aria-label="Open menu"
          >
            <Menu />
          </Button>
          <span className="text-sm font-semibold md:hidden">NeuroGuard</span>
          <div className="ml-auto flex items-center gap-3">
            <div className="hidden text-right leading-tight sm:block">
              <p className="text-sm font-medium" title={user.email}>
                {user.name || user.email}
              </p>
              <p className="text-xs text-muted-foreground">{describeRole(user.role, user.scope)}</p>
            </div>
            <span className="grid h-9 w-9 place-items-center rounded-full bg-accent text-xs font-semibold text-accent-foreground ring-1 ring-primary/30">
              {initials(user.name || user.email)}
            </span>
            <Button
              variant="ghost"
              size="icon"
              onClick={logout}
              aria-label="Log out"
              title="Log out"
            >
              <LogOut />
            </Button>
          </div>
        </header>
        <main className="relative flex-1 px-4 py-8 sm:px-6">
          <div className="pointer-events-none absolute inset-0 bg-hero" />
          <div key={location.pathname} className="relative mx-auto max-w-6xl animate-fade-up">
            <Outlet />
          </div>
        </main>
      </div>
    </div>
  )
}
