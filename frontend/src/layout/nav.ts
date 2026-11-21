import { isAllowed, type AccessSubject } from '../auth/access'
import { features } from '../features'

export interface NavItem {
  label: string
  to: string
}

const DASHBOARD: NavItem = { label: 'Dashboard', to: '/' }

/** Only the areas this user's role/scope may use (a convenience: the backend enforces the real rules). */
export function getAreas(user: AccessSubject | null): NavItem[] {
  return features
    .flatMap((f) => f.routes)
    .filter((r) => r.navLabel && isAllowed(user, r.access))
    .map((r) => ({ label: r.navLabel!, to: r.path }))
}

/** Sidebar items: the dashboard first, then the user's areas. */
export function getNavItems(user: AccessSubject | null): NavItem[] {
  return [DASHBOARD, ...getAreas(user)]
}
