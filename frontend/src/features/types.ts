import type { ReactNode } from 'react'
import type { Access } from '../auth/access'

export interface FeatureRoute {
  /** Absolute path, e.g. "/appointments" or "/patients/:patientId". "/" is the index route. */
  path: string
  element: ReactNode
  /** Who may open it. Every route is wrapped in <RequireRole> using this, so it can't be forgotten. */
  access: Access
  /** Set to show the route in the sidebar (only for users the access rule allows). */
  navLabel?: string
  /**
   * Optional: who sees the sidebar entry, when that differs from who may open the page
   * (e.g. readers may open /scanners from a link, but only some roles get it in the menu). Defaults to `access`.
   */
  navAccess?: Access
}

/**
 * A feature owns its routes and menu entries. To add a page: create it in the feature's folder and
 * add one entry to that feature's `routes`. Shared files (routes.tsx, Sidebar) don't need to change.
 */
export interface Feature {
  id: string
  routes: FeatureRoute[]
}
