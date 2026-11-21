import { NavLink } from 'react-router-dom'
import { useAuth } from '../auth/context'
import { getNavItems } from './nav'

export function Sidebar() {
  const { user } = useAuth()
  const items = getNavItems(user)

  return (
    <aside className="hidden w-60 shrink-0 border-r border-slate-200 bg-white md:block">
      <div className="px-5 py-5 text-lg font-semibold text-accent-600">NeuroGuard</div>
      <nav aria-label="Main">
        <ul className="space-y-1 px-3">
          {items.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                end={item.to === '/'}
                className={({ isActive }) =>
                  `block rounded-md px-3 py-2 text-sm font-medium ${
                    isActive
                      ? 'bg-accent-50 text-accent-700'
                      : 'text-slate-700 hover:bg-slate-100 hover:text-slate-900'
                  }`
                }
              >
                {item.label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </aside>
  )
}
