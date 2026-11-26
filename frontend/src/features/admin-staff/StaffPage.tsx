import { useState, type FormEvent } from 'react'
import { toast } from 'sonner'
import { Alert, AlertDescription, AlertTitle } from '@/components/ui/alert'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { PageHeader } from '@/components/app/PageHeader'
import { toApiError } from '@/api/errors'
import {
  BASE_ROLES,
  PERMISSION_SCOPES,
  type BaseRole,
  type PermissionScope,
  type StaffMember,
  type StaffSortBy,
} from '@/api/types'
import { ROLE_LABEL, SCOPE_LABEL } from '@/auth/labels'
import { useChangeStaffRole, useCreateStaff, useStaffList } from './api'

const PAGE_SIZES = [10, 20, 50]

export function StaffPage() {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [sortBy, setSortBy] = useState<StaffSortBy>('name')
  const [adding, setAdding] = useState(false)
  const [target, setTarget] = useState<StaffMember | null>(null)

  const list = useStaffList({ page, size, sortBy })

  const totalPages = Math.max(1, Math.ceil((list.data?.totalElements ?? 0) / size))

  const sortHead = (key: StaffSortBy, label: string) => (
    <button
      className={`font-medium cursor-pointer ${sortBy === key ? 'text-primary' : ''}`}
      onClick={() => {
        setSortBy(key)
        setPage(0)
      }}
    >
      {label}
      {sortBy === key ? ' ↑' : ''}
    </button>
  )

  return (
    <>
      <PageHeader
        title="Staff management"
        description="Create staff accounts and change their roles. Sorting is ascending."
        actions={<Button onClick={() => setAdding(true)}>Add staff</Button>}
      />

      {list.isPending ? (
        <p className="text-sm text-muted-foreground">Loading staff…</p>
      ) : list.isError ? (
        <p className="text-sm text-destructive">{toApiError(list.error).message}</p>
      ) : list.data.content.length === 0 ? (
        <div className="rounded-lg border border-dashed border-input bg-card px-6 py-12 text-center">
          <p className="font-medium">No staff accounts yet</p>
          <p className="mt-1 text-sm text-muted-foreground">
            Add the first staff member to get started.
          </p>
        </div>
      ) : (
        <>
          <div className="rounded-lg border border-border bg-card shadow-card">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{sortHead('name', 'Name')}</TableHead>
                  <TableHead>{sortHead('email', 'Email')}</TableHead>
                  <TableHead>Role</TableHead>
                  <TableHead>Permission scope</TableHead>
                  <TableHead>{sortHead('createdAt', 'Created')}</TableHead>
                  <TableHead>Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {list.data.content.map((s) => (
                  <TableRow key={s.id}>
                    <TableCell className="font-medium">{s.name}</TableCell>
                    <TableCell>{s.email}</TableCell>
                    <TableCell>{ROLE_LABEL[s.baseRole]}</TableCell>
                    <TableCell>
                      {s.permissionScope ? SCOPE_LABEL[s.permissionScope] : '—'}
                    </TableCell>
                    <TableCell>{new Date(s.createdAt).toLocaleDateString()}</TableCell>
                    <TableCell>
                      <Button
                        variant="secondary"
                        size="sm"
                        aria-label={`Change role for ${s.name}`}
                        onClick={() => setTarget(s)}
                      >
                        Change role
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>

          <div className="mt-3 flex flex-wrap items-center justify-between gap-3 text-sm text-muted-foreground">
            <span>
              Showing {page * size + 1}–{page * size + list.data.content.length} of{' '}
              {list.data.totalElements}
            </span>
            <div className="flex items-center gap-3">
              <label className="flex items-center gap-2">
                Rows per page
                <select
                  value={size}
                  onChange={(e) => {
                    setSize(Number(e.target.value))
                    setPage(0)
                  }}
                  className="rounded-md border border-input bg-card px-2 py-1"
                >
                  {PAGE_SIZES.map((n) => (
                    <option key={n} value={n}>
                      {n}
                    </option>
                  ))}
                </select>
              </label>
              <Button
                variant="secondary"
                size="sm"
                disabled={page === 0}
                onClick={() => setPage(page - 1)}
              >
                Previous
              </Button>
              <span>
                Page {page + 1} of {totalPages}
              </span>
              <Button
                variant="secondary"
                size="sm"
                disabled={page + 1 >= totalPages}
                onClick={() => setPage(page + 1)}
              >
                Next
              </Button>
            </div>
          </div>
        </>
      )}

      <Dialog open={adding} onOpenChange={setAdding}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Add staff</DialogTitle>
          </DialogHeader>
          <StaffCreateForm onDone={() => setAdding(false)} />
        </DialogContent>
      </Dialog>

      <Dialog open={target !== null} onOpenChange={(o) => !o && setTarget(null)}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Change role</DialogTitle>
          </DialogHeader>
          {target && <StaffChangeRoleForm staff={target} onDone={() => setTarget(null)} />}
        </DialogContent>
      </Dialog>
    </>
  )
}

function RoleFields({
  role,
  scope,
  setRole,
  setScope,
}: {
  role: BaseRole
  scope: string
  setRole: (r: BaseRole) => void
  setScope: (s: string) => void
}) {
  return (
    <>
      <div className="space-y-1.5">
        <Label htmlFor="role">Role</Label>
        <select
          id="role"
          value={role}
          onChange={(e) => setRole(e.target.value as BaseRole)}
          className="w-full rounded-md border border-input bg-card px-3 py-2 text-sm"
        >
          {BASE_ROLES.map((r) => (
            <option key={r} value={r}>
              {ROLE_LABEL[r]}
            </option>
          ))}
        </select>
      </div>
      {role === 'CLINICAL_STAFF' && (
        <div className="space-y-1.5">
          <Label htmlFor="scope">Permission scope</Label>
          <select
            id="scope"
            value={scope}
            onChange={(e) => setScope(e.target.value)}
            className="w-full rounded-md border border-input bg-card px-3 py-2 text-sm"
          >
            <option value="">Select…</option>
            {PERMISSION_SCOPES.map((s) => (
              <option key={s} value={s}>
                {SCOPE_LABEL[s]}
              </option>
            ))}
          </select>
        </div>
      )}
    </>
  )
}

const PASSWORD_MIN = 8
const PASSWORD_MAX = 72

function StaffCreateForm({ onDone }: { onDone: () => void }) {
  const create = useCreateStaff()
  const [f, setF] = useState({ name: '', email: '', password: '' })
  const [role, setRole] = useState<BaseRole>('CLINICAL_STAFF')
  const [scope, setScope] = useState<string>('')
  const [busy, setBusy] = useState(false)
  const [passwordInvalid, setPasswordInvalid] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    // Same rule as the backend (CreateStaffRequest): 8 to 72 characters.
    if (f.password.length < PASSWORD_MIN || f.password.length > PASSWORD_MAX) {
      setPasswordInvalid(true)
      return
    }
    setPasswordInvalid(false)
    setBusy(true)
    try {
      const res = await create.mutateAsync({
        name: f.name,
        email: f.email,
        password: f.password,
        baseRole: role,
        permissionScope: role === 'CLINICAL_STAFF' && scope ? (scope as PermissionScope) : null,
      })
      toast.success(`Added ${res.name}`)
      onDone()
    } catch (err) {
      const apiError = toApiError(err)
      if (apiError.fieldErrors.some((fe) => fe.field === 'password')) {
        setPasswordInvalid(true)
      } else {
        toast.error(apiError.message || 'Failed to add staff')
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-4">
      {passwordInvalid && (
        <Alert variant="destructive">
          <AlertTitle>Password does not meet the rules</AlertTitle>
          <AlertDescription>
            The password must be between {PASSWORD_MIN} and {PASSWORD_MAX} characters long.
          </AlertDescription>
        </Alert>
      )}
      {(['name', 'email', 'password'] as const).map((k) => (
        <div key={k} className="space-y-1.5">
          <Label htmlFor={k} className="capitalize">
            {k}
          </Label>
          <Input
            id={k}
            type={k === 'password' ? 'password' : k === 'email' ? 'email' : 'text'}
            value={f[k]}
            onChange={(e) => setF({ ...f, [k]: e.target.value })}
          />
        </div>
      ))}
      <RoleFields role={role} scope={scope} setRole={setRole} setScope={setScope} />
      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onDone}>
          Cancel
        </Button>
        <Button type="submit" disabled={busy}>
          Add staff
        </Button>
      </div>
    </form>
  )
}

function StaffChangeRoleForm({ staff, onDone }: { staff: StaffMember; onDone: () => void }) {
  const changeRole = useChangeStaffRole()
  const [role, setRole] = useState<BaseRole>(staff.baseRole)
  const [scope, setScope] = useState<string>(staff.permissionScope ?? '')
  const [busy, setBusy] = useState(false)

  async function submit(e: FormEvent) {
    e.preventDefault()
    setBusy(true)
    try {
      await changeRole.mutateAsync({
        id: staff.id,
        input: {
          baseRole: role,
          permissionScope: role === 'CLINICAL_STAFF' && scope ? (scope as PermissionScope) : null,
        },
      })
      toast.success(`Updated role for ${staff.name}`)
      onDone()
    } catch (err) {
      toast.error(toApiError(err).message || 'Failed to change role')
    } finally {
      setBusy(false)
    }
  }

  return (
    <form onSubmit={submit} noValidate className="space-y-4">
      <p className="text-sm text-muted-foreground">
        {staff.name} · {staff.email}
      </p>
      <RoleFields role={role} scope={scope} setRole={setRole} setScope={setScope} />
      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onDone}>
          Cancel
        </Button>
        <Button type="submit" disabled={busy}>
          Save
        </Button>
      </div>
    </form>
  )
}
