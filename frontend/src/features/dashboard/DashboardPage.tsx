import { Link } from 'react-router-dom'
import { ArrowUpRight } from 'lucide-react'
import { getAreas } from '@/auth/access'
import { useAuth } from '@/auth/context'
import { describeRole } from '@/auth/labels'
import { FallbackIcon, NAV_ICON, groupAreas } from '@/components/app/nav-meta'

function greeting() {
  const h = new Date().getHours()
  return h < 12 ? 'Good morning' : h < 18 ? 'Good afternoon' : 'Good evening'
}

export function DashboardPage() {
  const { user } = useAuth()
  if (!user) return null

  const groups = groupAreas(getAreas(user))
  const today = new Date().toLocaleDateString(undefined, {
    weekday: 'long',
    month: 'long',
    day: 'numeric',
  })
  let i = 0

  return (
    <>
      <section className="relative mb-10 overflow-hidden rounded-3xl border border-border bg-card p-8 shadow-card">
        <div className="absolute inset-0 bg-grid opacity-50 [mask-image:linear-gradient(to_left,black,transparent_70%)]" />
        <div className="absolute -right-10 -top-10 h-56 w-56 rounded-full bg-primary/20 blur-3xl" />
        <div className="relative">
          <p className="text-xs font-semibold uppercase tracking-[0.14em] text-primary">{today}</p>
          <h1 className="mt-2 text-3xl font-semibold tracking-tight sm:text-4xl">
            {greeting()}
            {user.name ? (
              <>
                , <span className="text-gradient">{user.name.split(' ')[0]}</span>
              </>
            ) : (
              ''
            )}
          </h1>
          <p className="mt-2 text-sm text-muted-foreground">
            Signed in as{' '}
            <span className="font-medium text-foreground">{describeRole(user.role, user.scope)}</span>
            . Pick up where you left off.
          </p>
        </div>
      </section>

      {groups.length === 0 ? (
        <p className="text-sm text-muted-foreground">There is nothing for your role here yet.</p>
      ) : (
        <div className="space-y-8">
          {groups.map(([group, items]) => (
            <section key={group}>
              <h2 className="mb-3 text-[11px] font-semibold uppercase tracking-[0.14em] text-muted-foreground">
                {group}
              </h2>
              <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {items.map((area) => {
                  const Icon = NAV_ICON[area.to] ?? FallbackIcon
                  return (
                    <li
                      key={area.to}
                      className="animate-fade-up"
                      style={{ animationDelay: `${i++ * 40}ms` }}
                    >
                      <Link
                        to={area.to}
                        className="group flex items-center gap-4 rounded-2xl border border-border bg-card p-4 shadow-card transition-all hover:-translate-y-0.5 hover:border-primary/40 hover:shadow-glow"
                      >
                        <span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl bg-accent text-accent-foreground ring-1 ring-primary/20 transition-colors group-hover:bg-gradient-primary group-hover:text-primary-foreground">
                          <Icon className="h-5 w-5" />
                        </span>
                        <span className="flex-1 text-sm font-medium">{area.label}</span>
                        <ArrowUpRight className="h-4 w-4 text-muted-foreground transition-all group-hover:translate-x-0.5 group-hover:-translate-y-0.5 group-hover:text-primary" />
                      </Link>
                    </li>
                  )
                })}
              </ul>
            </section>
          ))}
        </div>
      )}
    </>
  )
}
