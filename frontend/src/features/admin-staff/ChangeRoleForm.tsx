import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { FormProvider, useForm } from 'react-hook-form'
import { applyFieldErrors, toApiError } from '../../api/errors'
import type { ChangeRoleInput, StaffMember } from '../../api/types'
import { Button } from '../../components/Button'
import { RoleScopeFields } from './RoleScopeFields'
import { changeRoleSchema, toChangeRoleInput, type ChangeRoleValues } from './schema'

const FIELDS = ['baseRole', 'permissionScope'] as const

interface ChangeRoleFormProps {
  staff: StaffMember
  /** Should throw (the axios error) when the backend rejects the request. */
  onSubmit: (input: ChangeRoleInput) => Promise<void>
  onCancel: () => void
}

export function ChangeRoleForm({ staff, onSubmit, onCancel }: ChangeRoleFormProps) {
  const [formErrors, setFormErrors] = useState<string[]>([])
  const form = useForm<ChangeRoleValues>({
    resolver: zodResolver(changeRoleSchema),
    defaultValues: { baseRole: staff.baseRole, permissionScope: staff.permissionScope ?? '' },
  })
  const {
    handleSubmit,
    setError,
    formState: { isSubmitting },
  } = form

  const submit = handleSubmit(async (values) => {
    setFormErrors([])
    try {
      await onSubmit(toChangeRoleInput(values))
    } catch (err) {
      // e.g. 409 "Cannot demote the last remaining ADMIN" has no field, so it shows at the top
      setFormErrors(applyFieldErrors(toApiError(err), setError, FIELDS))
    }
  })

  return (
    <FormProvider {...form}>
      <form onSubmit={submit} noValidate className="space-y-4">
        <p className="text-sm text-slate-600">
          Changing the role of <strong>{staff.name}</strong> ({staff.email}). Their current session
          keeps the old role until it expires.
        </p>
        {formErrors.length > 0 && (
          <div
            role="alert"
            className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
          >
            {formErrors.map((message) => (
              <p key={message}>{message}</p>
            ))}
          </div>
        )}
        <RoleScopeFields />
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
          <Button type="submit" loading={isSubmitting}>
            Save role
          </Button>
        </div>
      </form>
    </FormProvider>
  )
}
