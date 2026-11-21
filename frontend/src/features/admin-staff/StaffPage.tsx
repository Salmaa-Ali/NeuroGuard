import { useState } from 'react'
import { toApiError } from '../../api/errors'
import type { ChangeRoleInput, CreateStaffInput, StaffMember, StaffSortBy } from '../../api/types'
import { ROLE_LABEL, SCOPE_LABEL } from '../../auth/labels'
import { Button } from '../../components/Button'
import { Dialog } from '../../components/Dialog'
import { EmptyState } from '../../components/EmptyState'
import { ErrorState } from '../../components/ErrorState'
import { LoadingState } from '../../components/LoadingState'
import { PageHeader } from '../../components/PageHeader'
import { Table, type Column } from '../../components/Table'
import { useToast } from '../../components/toast-context'
import { useChangeStaffRole, useCreateStaff, useStaffList } from './api'
import { ChangeRoleForm } from './ChangeRoleForm'
import { StaffForm } from './StaffForm'

const PAGE_SIZES = [10, 20, 50]

export function StaffPage() {
  const toast = useToast()
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [sortBy, setSortBy] = useState<StaffSortBy>('name')
  const [adding, setAdding] = useState(false)
  const [roleTarget, setRoleTarget] = useState<StaffMember | null>(null)

  const list = useStaffList({ page, size, sortBy })
  const create = useCreateStaff()
  const changeRole = useChangeStaffRole()

  async function handleCreate(input: CreateStaffInput) {
    try {
      const created = await create.mutateAsync(input)
      toast.success(`Added ${created.name}`)
      setAdding(false)
    } catch (err) {
      toast.error(toApiError(err).message)
      throw err // the form shows field errors
    }
  }

  async function handleChangeRole(input: ChangeRoleInput) {
    if (!roleTarget) return
    try {
      await changeRole.mutateAsync({ id: roleTarget.id, input })
      toast.success(`Updated role for ${roleTarget.name}`)
      setRoleTarget(null)
    } catch (err) {
      toast.error(toApiError(err).message)
      throw err
    }
  }

  const columns: Column<StaffMember>[] = [
    {
      key: 'name',
      header: 'Name',
      sortKey: 'name',
      cell: (s) => <span className="font-medium">{s.name}</span>,
    },
    { key: 'email', header: 'Email', sortKey: 'email', cell: (s) => s.email },
    { key: 'role', header: 'Role', cell: (s) => ROLE_LABEL[s.baseRole] },
    {
      key: 'scope',
      header: 'Permission scope',
      cell: (s) => (s.permissionScope ? SCOPE_LABEL[s.permissionScope] : '—'),
    },
    {
      key: 'created',
      header: 'Created',
      sortKey: 'createdAt',
      cell: (s) => new Date(s.createdAt).toLocaleDateString(),
    },
    {
      key: 'actions',
      header: 'Actions',
      cell: (s) => (
        <Button
          variant="secondary"
          className="px-2.5 py-1"
          aria-label={`Change role for ${s.name}`}
          onClick={() => setRoleTarget(s)}
        >
          Change role
        </Button>
      ),
    },
  ]

  function renderBody() {
    if (list.isPending) return <LoadingState label="Loading staff…" />
    if (list.isError) return <ErrorState error={list.error} onRetry={() => void list.refetch()} />
    const data = list.data
    if (data.content.length === 0) {
      return (
        <EmptyState
          title="No staff accounts yet"
          description="Add the first staff member to get started."
          action={<Button onClick={() => setAdding(true)}>Add staff</Button>}
        />
      )
    }
    const from = data.page * data.size + 1
    const to = from + data.content.length - 1
    return (
      <>
        <Table
          caption="Staff accounts"
          columns={columns}
          rows={data.content}
          rowKey={(s) => s.id}
          sortBy={sortBy}
          onSort={(key) => {
            setSortBy(key as StaffSortBy)
            setPage(0)
          }}
        />
        <div className="mt-3 flex flex-wrap items-center justify-between gap-3 text-sm text-slate-600">
          <span aria-live="polite">
            Showing {from}–{to} of {data.totalElements}
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
                className="rounded-md border border-slate-300 bg-white px-2 py-1"
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
              disabled={data.page === 0}
              onClick={() => setPage(page - 1)}
            >
              Previous
            </Button>
            <span>
              Page {data.page + 1} of {Math.max(data.totalPages, 1)}
            </span>
            <Button
              variant="secondary"
              disabled={data.page + 1 >= data.totalPages}
              onClick={() => setPage(page + 1)}
            >
              Next
            </Button>
          </div>
        </div>
      </>
    )
  }

  return (
    <>
      <PageHeader
        title="Staff management"
        description="Create staff accounts and change their roles. Sorting is ascending."
        actions={<Button onClick={() => setAdding(true)}>Add staff</Button>}
      />
      {renderBody()}

      <Dialog open={adding} onClose={() => setAdding(false)} title="Add staff">
        <StaffForm onSubmit={handleCreate} onCancel={() => setAdding(false)} />
      </Dialog>
      <Dialog open={roleTarget !== null} onClose={() => setRoleTarget(null)} title="Change role">
        {roleTarget && (
          <ChangeRoleForm
            staff={roleTarget}
            onSubmit={handleChangeRole}
            onCancel={() => setRoleTarget(null)}
          />
        )}
      </Dialog>
    </>
  )
}
