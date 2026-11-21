import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Navigate, useLocation } from 'react-router-dom'
import { z } from 'zod'
import { applyFieldErrors, toApiError } from '../../api/errors'
import type { LoginKind } from '../../auth/api'
import { useAuth } from '../../auth/context'
import { getLogoutReason } from '../../auth/session'
import { Button } from '../../components/Button'
import { Input } from '../../components/Input'

const loginSchema = z.object({
  email: z.string().trim().min(1, 'Email is required').pipe(z.email('Enter a valid email')),
  password: z.string().min(1, 'Password is required'),
})
type LoginValues = z.infer<typeof loginSchema>

const KINDS: { value: LoginKind; label: string }[] = [
  { value: 'staff', label: 'Staff' },
  { value: 'patient', label: 'Patient' },
]

export function LoginPage() {
  const { user, login } = useAuth()
  const location = useLocation()
  const [kind, setKind] = useState<LoginKind>('staff')
  const [formErrors, setFormErrors] = useState<string[]>([])
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<LoginValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', password: '' },
  })

  // Already signed in (or just signed in): go where the user was heading.
  if (user) {
    const from = (location.state as { from?: string } | null)?.from
    return <Navigate to={from ?? '/'} replace />
  }

  const submit = handleSubmit(async (values) => {
    setFormErrors([])
    try {
      await login(kind, values)
    } catch (err) {
      setFormErrors(applyFieldErrors(toApiError(err), setError, ['email', 'password']))
    }
  })

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="mb-6 text-center">
          <p className="text-2xl font-semibold text-accent-600">NeuroGuard</p>
          <p className="mt-1 text-sm text-slate-600">Neurology MRI care platform</p>
        </div>

        <form
          onSubmit={submit}
          noValidate
          aria-labelledby="login-title"
          className="space-y-4 rounded-xl border border-slate-200 bg-white p-6 shadow-sm"
        >
          <h1 id="login-title" className="text-lg font-semibold">
            Log in
          </h1>

          {getLogoutReason() === 'expired' && (
            <p role="status" className="rounded-md bg-amber-50 px-3 py-2 text-sm text-amber-900">
              Session expired. Please log in again.
            </p>
          )}

          <fieldset>
            <legend className="mb-1 text-sm font-medium text-slate-700">I am a</legend>
            <div className="grid grid-cols-2 gap-2">
              {KINDS.map((k) => (
                <label
                  key={k.value}
                  className={`cursor-pointer rounded-md border px-3 py-2 text-center text-sm font-medium has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-offset-2 has-[:focus-visible]:outline-accent-600 ${
                    kind === k.value
                      ? 'border-accent-600 bg-accent-50 text-accent-700'
                      : 'border-slate-300 text-slate-700 hover:bg-slate-50'
                  }`}
                >
                  <input
                    type="radio"
                    name="kind"
                    value={k.value}
                    checked={kind === k.value}
                    onChange={() => setKind(k.value)}
                    className="sr-only"
                  />
                  {k.label}
                </label>
              ))}
            </div>
          </fieldset>

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
            label="Email"
            type="email"
            autoComplete="username"
            error={errors.email?.message}
            {...register('email')}
          />
          <Input
            label="Password"
            type="password"
            autoComplete="current-password"
            error={errors.password?.message}
            {...register('password')}
          />
          <Button type="submit" loading={isSubmitting} className="w-full">
            Log in
          </Button>
        </form>
      </div>
    </div>
  )
}
