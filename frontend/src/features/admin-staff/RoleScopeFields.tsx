import { useFormContext } from 'react-hook-form'
import { BASE_ROLES, PERMISSION_SCOPES, type BaseRole, type PermissionScope } from '../../api/types'
import { ROLE_LABEL, SCOPE_LABEL } from '../../auth/labels'
import { Select } from '../../components/Select'

interface RoleScopeValues {
  baseRole: BaseRole
  permissionScope: PermissionScope | ''
}

/** Role dropdown plus the scope dropdown, which only exists for CLINICAL_STAFF. Needs a <FormProvider>. */
export function RoleScopeFields() {
  const {
    register,
    watch,
    formState: { errors },
  } = useFormContext<RoleScopeValues>()
  const role = watch('baseRole')

  return (
    <>
      <Select label="Role" error={errors.baseRole?.message} {...register('baseRole')}>
        {BASE_ROLES.map((r) => (
          <option key={r} value={r}>
            {ROLE_LABEL[r]}
          </option>
        ))}
      </Select>
      {role === 'CLINICAL_STAFF' && (
        <Select
          label="Permission scope"
          error={errors.permissionScope?.message}
          {...register('permissionScope')}
        >
          <option value="">Select a scope…</option>
          {PERMISSION_SCOPES.map((s) => (
            <option key={s} value={s}>
              {SCOPE_LABEL[s]}
            </option>
          ))}
        </Select>
      )}
    </>
  )
}
