import type { PermissionScope, Role } from '../api/types'

/**
 * Who may use something. A user is allowed if their role is in `roles` OR their permission scope is
 * in `scopes`. No rule (or an empty one) means any logged-in user.
 *
 * This is only a convenience for hiding menu items and pages: the backend does the real checks.
 */
export interface Access {
  roles?: readonly Role[]
  scopes?: readonly PermissionScope[]
}

export interface AccessSubject {
  role: Role
  scope: PermissionScope | null
}

export function isAllowed(user: AccessSubject | null | undefined, access?: Access): boolean {
  if (!user) return false
  const { roles, scopes } = access ?? {}
  if (!roles?.length && !scopes?.length) return true
  if (roles?.includes(user.role)) return true
  return user.scope !== null && !!scopes?.includes(user.scope)
}

/**
 * The permission matrix for the UI (mirrors docs/permissions.md and the per-role menus).
 * Features reference these names instead of writing role lists themselves, so a rule change
 * happens in one place.
 */
export const ACCESS = {
  /** Any logged-in user. */
  anyone: {},
  admin: { roles: ['ADMIN'] },
  patient: { roles: ['PATIENT'] },

  // Menu visibility per feature (see the role lists in the project brief)
  patientsMenu: { scopes: ['NEUROLOGIST', 'RADIOLOGIST', 'BILLING_CLERK'] },
  appointments: { scopes: ['NEUROLOGIST'] },
  mriOrders: { scopes: ['NEUROLOGIST', 'MRI_TECHNOLOGIST'] },
  clinical: { scopes: ['NEUROLOGIST'] },
  studies: { scopes: ['RADIOLOGIST'] },
  dicomUpload: { scopes: ['MRI_TECHNOLOGIST'] },
  reports: { roles: ['ADMIN'], scopes: ['RADIOLOGIST'] },
  scanners: { scopes: ['BIOMEDICAL_ENGINEER'] },
  invoices: { scopes: ['BILLING_CLERK'] },
  payments: { scopes: ['BILLING_CLERK'] },
  auditLog: { roles: ['ADMIN'] },

  /** GET /api/patients/{id}: clinical staff with these scopes (patients use "My profile"). */
  patientRecord: {
    scopes: ['NEUROLOGIST', 'RADIOLOGIST', 'MRI_TECHNOLOGIST', 'BILLING_CLERK'],
  },
} as const satisfies Record<string, Access>
