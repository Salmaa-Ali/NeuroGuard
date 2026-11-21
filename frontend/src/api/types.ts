/**
 * Domain types derived from the generated OpenAPI types (schema.d.ts, regenerate with `npm run gen:api`).
 *
 * The backend does not mark response fields as required, so the generator makes them all optional.
 * These helpers make them required again and mark the fields the API really can send as null.
 * If the backend contract changes, the derivation below stops compiling and shows what to fix.
 */
import type { components } from './schema'

type S = components['schemas']

type NullableFields<T, K extends keyof T> = Omit<Required<T>, K> & {
  [P in K]-?: Required<T>[P] | null
}

export type UserType = NonNullable<S['LoginResponse']['userType']>
export type Role = NonNullable<S['LoginResponse']['role']>
export type BaseRole = S['CreateStaffRequest']['baseRole']
export type PermissionScope = NonNullable<S['CreateStaffRequest']['permissionScope']>

export type LoginResult = NullableFields<S['LoginResponse'], 'scope'>
export type Me = NullableFields<S['MeResponse'], 'scope'>
export type StaffMember = NullableFields<S['StaffResponse'], 'permissionScope'>
export type PatientSummary = NullableFields<S['PatientSummary'], 'phone' | 'neurologyHistory'>
export type StaffPage = Omit<Required<S['PageResponseStaffResponse']>, 'content'> & {
  content: StaffMember[]
}

export type LoginInput = S['LoginRequest']
export type CreateStaffInput = Omit<S['CreateStaffRequest'], 'permissionScope'> & {
  permissionScope: PermissionScope | null
}
export type ChangeRoleInput = Omit<S['ChangeStaffRoleRequest'], 'permissionScope'> & {
  permissionScope: PermissionScope | null
}

/** Backend only supports ascending sort on these columns. */
export type StaffSortBy = 'name' | 'email' | 'createdAt'

export const BASE_ROLES: BaseRole[] = ['ADMIN', 'CLINICAL_STAFF']
export const PERMISSION_SCOPES: PermissionScope[] = [
  'NEUROLOGIST',
  'RADIOLOGIST',
  'MRI_TECHNOLOGIST',
  'BIOMEDICAL_ENGINEER',
  'BILLING_CLERK',
]
