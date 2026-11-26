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
  appointments: { scopes: ['NEUROLOGIST', 'RADIOLOGIST', 'MRI_TECHNOLOGIST', 'BILLING_CLERK'] },
  mriOrders: { roles: ['PATIENT'], scopes: ['NEUROLOGIST', 'RADIOLOGIST', 'MRI_TECHNOLOGIST'] },
  clinical: { scopes: ['NEUROLOGIST'] },
  studies: { scopes: ['RADIOLOGIST'] },
  dicomUpload: { scopes: ['MRI_TECHNOLOGIST'] },
  reports: { roles: ['ADMIN'], scopes: ['RADIOLOGIST'] },
  /** Sidebar entry "Scanners & QA": biomedical engineer and admin. */
  scanners: { roles: ['ADMIN'], scopes: ['BIOMEDICAL_ENGINEER'] },
  /** Scanners & calibration, read (Authz.SCANNER_READ): admin + four clinical scopes; not billing clerk or patients. */
  scannersRead: {
    roles: ['ADMIN'],
    scopes: ['NEUROLOGIST', 'RADIOLOGIST', 'MRI_TECHNOLOGIST', 'BIOMEDICAL_ENGINEER'],
  },
  /** Scanners & calibration, write (Authz.SCANNER_WRITE): biomedical engineer only. */
  scannersWrite: { scopes: ['BIOMEDICAL_ENGINEER'] },
  invoices: { scopes: ['BILLING_CLERK'] },
  payments: { scopes: ['BILLING_CLERK'] },
  auditLog: { roles: ['ADMIN'] },

  /** GET /api/patients/{id}: clinical staff with these scopes (patients use "My profile"). */
  patientRecord: {
    scopes: ['NEUROLOGIST', 'RADIOLOGIST', 'MRI_TECHNOLOGIST', 'BILLING_CLERK'],
  },
} as const satisfies Record<string, Access>

export interface NavItem {
  label: string
  to: string
  access: Access
}

/** Sidebar order mirrors the original feature list. */
export const AREAS: NavItem[] = [
  { label: 'Staff management', to: '/admin/staff', access: ACCESS.admin },
  { label: 'Audit log', to: '/audit-log', access: ACCESS.auditLog },
  { label: 'Patients', to: '/patients', access: ACCESS.patientsMenu },
  { label: 'My profile', to: '/profile', access: ACCESS.patient },
  { label: 'Appointments', to: '/appointments', access: ACCESS.appointments },
  { label: 'My appointments', to: '/my/appointments', access: ACCESS.patient },
  { label: 'MRI orders', to: '/mri-orders', access: ACCESS.mriOrders },
  { label: 'MRI studies / Viewer', to: '/studies', access: ACCESS.studies },
  { label: 'DICOM upload', to: '/dicom-upload', access: ACCESS.dicomUpload },
  { label: 'Clinical', to: '/clinical', access: ACCESS.clinical },
  { label: 'My prescriptions', to: '/my/prescriptions', access: ACCESS.patient },
  { label: 'Reports', to: '/reports', access: ACCESS.reports },
  { label: 'My reports', to: '/my/reports', access: ACCESS.patient },
  { label: 'Scanners & QA', to: '/scanners', access: ACCESS.scanners },
  { label: 'Invoices', to: '/invoices', access: ACCESS.invoices },
  { label: 'Payments', to: '/payments', access: ACCESS.payments },
  { label: 'My invoices', to: '/my/invoices', access: ACCESS.patient },
]

export function getAreas(user: AccessSubject | null): NavItem[] {
  return AREAS.filter((a) => isAllowed(user, a.access))
}
