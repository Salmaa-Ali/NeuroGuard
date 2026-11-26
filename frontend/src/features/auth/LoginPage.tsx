import { useState, type FormEvent } from 'react'
import { matchPath, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { Brain, Eye, EyeOff } from 'lucide-react'
import { z } from 'zod'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { toApiError } from '@/api/errors'
import { useAuth } from '@/auth/context'
import { getLogoutReason, getSession } from '@/auth/session'
import { isAllowed, type AccessSubject } from '@/auth/access'
import { dashboard } from '@/features/dashboard/feature'
import { features } from '@/features'

function postLoginPath(from?: string, user: AccessSubject | null = getSession()) {
  if (!from || !user) return '/'
  const route = [...dashboard.routes, ...features.flatMap((feature) => feature.routes)].find((item) =>
    matchPath({ path: item.path, end: true }, from),
  )
  return route && isAllowed(user, route.access) ? from : '/'
}

type Mode = 'login' | 'register' 
const emailSchema = z.string().trim().min(1, 'Email is required').pipe(z.email('Enter a valid email'))

export function LoginPage() {
  const { user, login, registerPatient } = useAuth()
  const location = useLocation()
  const navigate = useNavigate()
  const [mode, setMode] = useState<Mode>('login')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [dateOfBirth, setDateOfBirth] = useState('')
  const [phone, setPhone] = useState('')
  const [neurologyHistory, setNeurologyHistory] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [notice, setNotice] = useState<string | null>(
    getLogoutReason() === 'expired' ? 'Session expired. Please log in again.' : null,
  )
  const [busy, setBusy] = useState(false)
  const [showPw, setShowPw] = useState(false)

  // Already signed in (or just signed in): go where the user was heading.
  if (user) {
    const from = (location.state as { from?: string } | null)?.from
    return <Navigate to={postLoginPath(from, user)} replace />
  }

  function switchMode(next: Mode) {
    setMode(next)
    setError(null)
    setNotice(null)
  }

  async function submit(e: FormEvent) {
    e.preventDefault()
    setError(null)
    setNotice(null)

    if (mode === 'register' && !name.trim()) {
      return setError('Full name is required')
    }

    const parsed = emailSchema.safeParse(email)
    if (!parsed.success) {
      return setError(parsed.error.issues[0]?.message ?? 'Invalid email')
    }
    const em = parsed.data.toLowerCase()

    if (mode === 'login' && !password) {
      return setError('Password is required')
    }

    if (mode === 'register') {
      if (password.length < 8 || password.length > 72) {
        return setError('Password must be 8–72 characters')
      }
      if (!dateOfBirth) {
        return setError('Date of birth is required')
      }
    }

    setBusy(true)
    try {
      if (mode === 'login') {
        await login({ email: em, password })
        const from = (location.state as { from?: string } | null)?.from
        navigate(postLoginPath(from), { replace: true })
      } else if (mode === 'register') {
        await registerPatient({
          name: name.trim(),
          email: em,
          password,
          dateOfBirth,
          phone: phone.trim() ? phone.trim() : null,
          neurologyHistory: neurologyHistory.trim() ? neurologyHistory.trim() : null,
        })
        navigate('/', { replace: true })
      } else {
        setNotice('If an account exists for that email, a reset link is on its way.')
      }
    } catch (err) {
      setError(
        toApiError(err).message ||
          (mode === 'register' ? 'Could not create patient account.' : 'Invalid email or password.'),
      )
    } finally {
      setBusy(false)
    }
  }

  const titles: Record<Mode, string> = {
    login: 'Log in',
    register: 'Create patient account',
  }

  return (
    <div className="grid min-h-screen bg-background lg:grid-cols-2">
      <aside className="relative hidden overflow-hidden border-r border-border bg-sidebar lg:flex lg:flex-col lg:justify-between lg:p-12">
        <div className="absolute inset-0 bg-grid opacity-70" />
        <div className="absolute inset-0 bg-hero" />
        <p className="relative flex items-center gap-2.5 text-lg font-semibold">
          <span className="grid h-9 w-9 place-items-center rounded-xl bg-gradient-primary text-primary-foreground shadow-glow">
            <Brain className="h-5 w-5" />
          </span>
          NeuroGuard
        </p>
        <div className="relative mx-auto my-10 grid h-64 w-64 place-items-center">
          <span className="absolute inset-0 animate-pulse-ring rounded-full border border-primary/40" />
          <span className="absolute inset-6 rounded-full border border-primary/20" />
          <div className="relative grid h-44 w-44 place-items-center overflow-hidden rounded-full bg-card/60 ring-1 ring-primary/30 backdrop-blur">
            <Brain className="h-24 w-24 text-primary" strokeWidth={1.2} />
          </div>
        </div>
        <div className="relative max-w-md">
          <h2 className="text-3xl font-semibold leading-tight tracking-tight">
            Neurology & MRI care, <span className="text-gradient">in one secure place.</span>
          </h2>
          <p className="mt-3 text-sm text-muted-foreground">
            Patients, imaging, clinical notes and billing, with access shaped to every role on your
            team.
          </p>
        </div>
      </aside>
      <div className="flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-sm animate-fade-up">
          <div className="mb-8 lg:hidden">
            <p className="flex items-center gap-2 text-xl font-semibold">
              <span className="grid h-8 w-8 place-items-center rounded-lg bg-gradient-primary text-primary-foreground">
                <Brain className="h-4 w-4" />
              </span>{' '}
              NeuroGuard
            </p>
          </div>
          <form onSubmit={submit} noValidate className="space-y-4">
            <div>
              <h1 className="text-2xl font-semibold tracking-tight">{titles[mode]}</h1>
              {mode === 'login' && (
                <p className="mt-1 text-sm text-muted-foreground">
                  Welcome back. Sign in to continue.
                </p>
              )}
              {mode === 'register' && (
                <p className="mt-1 text-sm text-muted-foreground">
                  Register as a patient to view your profile, prescriptions, and invoices.
                </p>
              )}
            </div>
            {error && (
              <p
                role="alert"
                className="rounded-md border border-destructive/30 bg-destructive/10 px-3 py-2 text-sm text-destructive"
              >
                {error}
              </p>
            )}
            {notice && (
              <p role="status" className="rounded-md bg-warning px-3 py-2 text-sm text-warning-foreground">
                {notice}
              </p>
            )}
            {mode === 'register' && (
              <div className="space-y-1.5">
                <Label htmlFor="name">Full name</Label>
                <Input
                  className="h-10"
                  id="name"
                  type="text"
                  autoComplete="name"
                  placeholder="Sara Ahmed"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                />
              </div>
            )}
            <div className="space-y-1.5">
              <Label htmlFor="email">Email</Label>
              <Input
                className="h-10"
                id="email"
                type="email"
                autoComplete="username"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            {(
              <div className="space-y-1.5">
                <Label htmlFor="password">Password</Label>
                <div className="relative">
                  <Input
                    className="h-10 pr-10"
                    id="password"
                    type={showPw ? 'text' : 'password'}
                    autoComplete={mode === 'register' ? 'new-password' : 'current-password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPw((v) => !v)}
                    aria-label={showPw ? 'Hide password' : 'Show password'}
                    className="absolute inset-y-0 right-0 grid w-10 place-items-center text-muted-foreground transition-colors hover:text-foreground cursor-pointer"
                  >
                    {showPw ? <Eye className="h-4 w-4" /> : <EyeOff className="h-4 w-4" />}
                  </button>
                </div>
              </div>
            )}
            {mode === 'register' && (
              <>
                <div className="grid gap-3 sm:grid-cols-2">
                  <div className="space-y-1.5">
                    <Label htmlFor="dob">Date of birth</Label>
                    <Input
                      className="h-10"
                      id="dob"
                      type="date"
                      value={dateOfBirth}
                      onChange={(e) => setDateOfBirth(e.target.value)}
                    />
                  </div>
                  <div className="space-y-1.5">
                    <Label htmlFor="phone">Phone (optional)</Label>
                    <Input
                      className="h-10"
                      id="phone"
                      type="tel"
                      placeholder="+20 10..."
                      value={phone}
                      onChange={(e) => setPhone(e.target.value)}
                    />
                  </div>
                </div>
                <div className="space-y-1.5">
                  <Label htmlFor="history">Neurology history (optional)</Label>
                  <Textarea
                    id="history"
                    rows={2}
                    placeholder="Prior migraines, seizures, medications…"
                    value={neurologyHistory}
                    onChange={(e) => setNeurologyHistory(e.target.value)}
                  />
                </div>
              </>
            )}
            <Button
              type="submit"
              disabled={busy}
              className="h-10 w-full bg-gradient-primary font-semibold shadow-glow transition-transform hover:-translate-y-px"
            >
              {busy
                ? 'Please wait…'
                : mode === 'login'
                  ? 'Log in'
                  : mode === 'register'
                    ? 'Create account'
                    : 'Send reset link'}
            </Button>
            <div className="flex flex-col gap-1.5 pt-1 text-center text-sm">
              {mode === 'login' && (
                <>
                  <button
                    type="button"
                    className="text-primary hover:underline cursor-pointer"
                    onClick={() => switchMode('register')}
                  >
                    New patient? Create an account
                  </button>
                </>
              )}
              {mode !== 'login' && (
                <button
                  type="button"
                  className="text-primary hover:underline cursor-pointer"
                  onClick={() => switchMode('login')}
                >
                  Back to log in
                </button>
              )}
            </div>
          </form>
        </div>
      </div>
    </div>
  )
}
