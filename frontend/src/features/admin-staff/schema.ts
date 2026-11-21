import { z } from 'zod'
import {
  BASE_ROLES,
  PERMISSION_SCOPES,
  type BaseRole,
  type ChangeRoleInput,
  type CreateStaffInput,
  type PermissionScope,
} from '../../api/types'

const baseRole = z.enum(BASE_ROLES as [BaseRole, ...BaseRole[]], { error: 'Select a role' })
// '' is the "nothing selected" value of the <select>
const permissionScope = z.union([
  z.enum(PERMISSION_SCOPES as [PermissionScope, ...PermissionScope[]]),
  z.literal(''),
])

/** Backend rule: CLINICAL_STAFF needs a permission scope; ADMIN has none. */
function requireScopeForClinicalStaff(
  value: { baseRole: BaseRole; permissionScope: PermissionScope | '' },
  ctx: z.RefinementCtx,
) {
  if (value.baseRole === 'CLINICAL_STAFF' && value.permissionScope === '') {
    ctx.addIssue({
      code: 'custom',
      path: ['permissionScope'],
      message: 'Select a permission scope',
    })
  }
}

export const createStaffSchema = z
  .object({
    name: z.string().trim().min(1, 'Name is required').max(255, 'Name is too long'),
    email: z.string().trim().min(1, 'Email is required').pipe(z.email('Enter a valid email')),
    password: z
      .string()
      .min(8, 'Password must be at least 8 characters')
      .max(72, 'Password must be at most 72 characters'),
    baseRole,
    permissionScope,
  })
  .superRefine(requireScopeForClinicalStaff)

export const changeRoleSchema = z
  .object({ baseRole, permissionScope })
  .superRefine(requireScopeForClinicalStaff)

export type CreateStaffValues = z.infer<typeof createStaffSchema>
export type ChangeRoleValues = z.infer<typeof changeRoleSchema>

/** ADMIN never sends a scope, whatever the hidden field still holds. */
const scopeFor = (v: { baseRole: BaseRole; permissionScope: PermissionScope | '' }) =>
  v.baseRole === 'CLINICAL_STAFF' && v.permissionScope !== '' ? v.permissionScope : null

export function toCreateInput(v: CreateStaffValues): CreateStaffInput {
  return {
    name: v.name,
    email: v.email,
    password: v.password,
    baseRole: v.baseRole,
    permissionScope: scopeFor(v),
  }
}

export function toChangeRoleInput(v: ChangeRoleValues): ChangeRoleInput {
  return { baseRole: v.baseRole, permissionScope: scopeFor(v) }
}
