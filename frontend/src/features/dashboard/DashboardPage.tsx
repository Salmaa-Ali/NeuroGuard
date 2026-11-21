import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/context'
import { describeRole } from '../../auth/labels'
import { PageHeader } from '../../components/PageHeader'
import { getAreas } from '../../layout/nav'

export function DashboardPage() {
  const { user } = useAuth()
  if (!user) return null

  const areas = getAreas(user)

  return (
    <>
      <PageHeader
        title={`Welcome${user.name ? `, ${user.name}` : ''}`}
        description={`You are signed in as ${describeRole(user.role, user.scope)}.`}
      />
      {areas.length === 0 ? (
        <p className="text-sm text-slate-600">There is nothing for your role here yet.</p>
      ) : (
        <>
          <h2 className="mb-3 text-sm font-semibold uppercase tracking-wide text-slate-500">
            Your areas
          </h2>
          <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {areas.map((area) => (
              <li key={area.to}>
                <Link
                  to={area.to}
                  className="block rounded-lg border border-slate-200 bg-white px-4 py-4 text-sm font-medium text-slate-800 shadow-sm hover:border-accent-600 hover:text-accent-700"
                >
                  {area.label}
                </Link>
              </li>
            ))}
          </ul>
        </>
      )}
    </>
  )
}
