import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'

export function ResetPasswordPage() {
  const navigate = useNavigate()
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    if (password.length < 8 || password.length > 72) {
      return setError('Password must be 8–72 characters')
    }
    setBusy(true)
    // Simulate reset or connect to future endpoint
    setBusy(false)
    navigate('/login', { replace: true })
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <form
        onSubmit={submit}
        noValidate
        className="w-full max-w-sm space-y-4 rounded-xl border border-border bg-card p-6 shadow-card"
      >
        <h1 className="text-lg font-semibold">Choose a new password</h1>
        {error && (
          <p role="alert" className="text-sm text-destructive">
            {error}
          </p>
        )}
        <div className="space-y-1.5">
          <Label htmlFor="pw">New password</Label>
          <Input
            id="pw"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>
        <Button type="submit" disabled={busy} className="w-full">
          Save password
        </Button>
      </form>
    </div>
  )
}
