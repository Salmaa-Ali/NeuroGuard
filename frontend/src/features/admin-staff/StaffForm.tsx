import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { FormProvider, useForm } from 'react-hook-form'
import { applyFieldErrors, toApiError } from '../../api/errors'
import type { CreateStaffInput } from '../../api/types'
import { Button } from '../../components/Button'
import { Input } from '../../components/Input'
import { RoleScopeFields } from './RoleScopeFields'
import { createStaffSchema, toCreateInput, type CreateStaffValues } from './schema'

const FIELDS = ['name', 'email', 'password', 'baseRole', 'permissionScope'] as const

interface StaffFormProps {
  /** Should throw (the axios error) when the backend rejects the request. */
  onSubmit: (input: CreateStaffInput) => Promise<void>
  onCancel: () => void
}

/** "Add staff" form. The scope dropdown is shown and required only for CLINICAL_STAFF. */
export function StaffForm({ onSubmit, onCancel }: StaffFormProps) {
  const [formErrors, setFormErrors] = useState<string[]>([])
  const form = useForm<CreateStaffValues>({
    resolver: zodResolver(createStaffSchema),
    defaultValues: {
      name: '',
      email: '',
      password: '',
      baseRole: 'CLINICAL_STAFF',
      permissionScope: '',
    },
  })
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = form

  const submit = handleSubmit(async (values) => {
    setFormErrors([])
    try {
      await onSubmit(toCreateInput(values))
    } catch (err) {
      const apiError = toApiError(err)
      if (apiError.status === 409) {
        // duplicate email: the only 409 this form can cause
        setError('email', { type: 'server', message: apiError.message })
        return
      }
      setFormErrors(applyFieldErrors(apiError, setError, FIELDS))
    }
  })

  return (
    <FormProvider {...form}>
      <form onSubmit={submit} noValidate className="space-y-4">
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
        <Input
          label="Full name"
          autoComplete="off"
          error={errors.name?.message}
          {...register('name')}
        />
        <Input
          label="Email"
          type="email"
          autoComplete="off"
          error={errors.email?.message}
          {...register('email')}
        />
        <Input
          label="Temporary password"
          type="password"
          autoComplete="new-password"
          hint="8 to 72 characters"
          error={errors.password?.message}
          {...register('password')}
        />
        <RoleScopeFields />
        <div className="flex justify-end gap-2 pt-2">
          <Button variant="secondary" onClick={onCancel}>
            Cancel
          </Button>
          <Button type="submit" loading={isSubmitting}>
            Add staff
          </Button>
        </div>
      </form>
    </FormProvider>
  )
}
