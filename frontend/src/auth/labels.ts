import type { BaseRole, PermissionScope, Role } from '../api/types'

export const SCOPE_LABEL: Record<PermissionScope, string> = {
  NEUROLOGIST: 'Neurologist',
  RADIOLOGIST: 'Radiologist',
  MRI_TECHNOLOGIST: 'MRI technologist',
  BIOMEDICAL_ENGINEER: 'Biomedical engineer',
  BILLING_CLERK: 'Billing clerk',
}

export const ROLE_LABEL: Record<Role | BaseRole, string> = {
  ADMIN: 'Administrator',
  CLINICAL_STAFF: 'Clinical staff',
  PATIENT: 'Patient',
}

/** Short human label for a user's role: the scope for clinical staff, otherwise the role. */
export function describeRole(role: Role, scope: PermissionScope | null): string {
  return role === 'CLINICAL_STAFF' && scope ? SCOPE_LABEL[scope] : ROLE_LABEL[role]
}
