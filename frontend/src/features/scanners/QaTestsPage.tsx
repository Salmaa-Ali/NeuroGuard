import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { toast } from 'sonner'
import { Pencil, Plus } from 'lucide-react'
import { applyFieldErrors, toApiError } from '@/api/errors'
import type { QaTest, QaTestInput } from '@/api/types'
import { DataCard, EmptyState, TableSkeleton, td, th } from '@/components/app/DataTable'
import { PageHeader } from '@/components/app/PageHeader'
import { ErrorState } from '@/components/ErrorState'
import { Input } from '@/components/Input'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { cn } from '@/lib/utils'
import { useQaTests, useSaveQaTest } from './api'
import { PLACEHOLDER_TEXT, formatLimits } from './qa'
import { qaTestSchema, toQaTestInput, type QaTestValues } from './schema'

/**
 * QA tests and their tolerances (biomedical engineer). Tolerances are data, never invented in code;
 * placeholders must be confirmed by a medical physicist. Tests are deactivated, never deleted, and editing a
 * tolerance does not change sessions already recorded.
 */
export function QaTestsPage() {
  const tests = useQaTests(false)
  const save = useSaveQaTest()
  const [editing, setEditing] = useState<QaTest | 'new' | null>(null)

  async function toggleActive(t: QaTest) {
    try {
      await save.mutateAsync({ id: t.id, input: { ...toInput(t), active: !t.active } })
      toast.success(t.active ? `Deactivated ${t.code}` : `Reactivated ${t.code}`)
    } catch (err) {
      toast.error(toApiError(err).message || 'Failed to update')
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Scanners & QA"
        title="QA tests"
        description="Checks performed in each QA session and their tolerances. Editing a tolerance only affects sessions recorded afterwards."
        actions={
          <Button onClick={() => setEditing('new')}>
            <Plus className="h-4 w-4" />
            Add QA test
          </Button>
        }
      />
      <p className="-mt-6 mb-6 text-sm">
        <Link to="/scanners" className="text-primary hover:underline">
          ← All scanners
        </Link>
      </p>
      <DataCard title="QA tests">
        {tests.isPending ? (
          <TableSkeleton />
        ) : tests.isError ? (
          <div className="p-5">
            <ErrorState error={tests.error} onRetry={() => tests.refetch()} />
          </div>
        ) : tests.data.length === 0 ? (
          <EmptyState text="No QA tests yet." />
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead className="bg-muted/40">
                <tr>
                  <th className={th}>Code</th>
                  <th className={th}>Name</th>
                  <th className={th}>Tolerance</th>
                  <th className={th}>Reference</th>
                  <th className={th}>Active</th>
                  <th className={cn(th, 'text-right')}>Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {tests.data.map((t) => (
                  <tr key={t.id} className={cn(!t.active && 'opacity-60')}>
                    <td className={cn(td, 'font-mono text-xs')}>{t.code}</td>
                    <td className={td}>{t.name}</td>
                    <td className={td}>
                      {formatLimits(t.lowerLimit, t.upperLimit, t.unit)}
                      {t.placeholder && (
                        <p className="mt-1 inline-flex rounded-full border border-warning-foreground/30 bg-warning px-2 py-0.5 text-xs font-medium text-warning-foreground">
                          {PLACEHOLDER_TEXT}
                        </p>
                      )}
                    </td>
                    <td className={cn(td, 'max-w-xs text-xs text-muted-foreground')}>
                      {t.reference ?? '—'}
                    </td>
                    <td className={td}>{t.active ? 'Yes' : 'No'}</td>
                    <td className={cn(td, 'whitespace-nowrap text-right')}>
                      <Button
                        variant="ghost"
                        size="sm"
                        aria-label={`Edit ${t.code}`}
                        onClick={() => setEditing(t)}
                      >
                        <Pencil className="h-3.5 w-3.5" />
                      </Button>
                      <Button variant="outline" size="sm" onClick={() => toggleActive(t)}>
                        {t.active ? 'Deactivate' : 'Reactivate'}
                      </Button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </DataCard>

      <Dialog open={editing !== null} onOpenChange={(o) => !o && setEditing(null)}>
        <DialogContent className="sm:max-w-lg">
          <DialogHeader>
            <DialogTitle>{editing === 'new' ? 'Add QA test' : 'Edit QA test'}</DialogTitle>
          </DialogHeader>
          {editing !== null && (
            <QaTestForm
              test={editing === 'new' ? undefined : editing}
              onCancel={() => setEditing(null)}
              onSubmit={async (input) => {
                await save.mutateAsync({ id: editing === 'new' ? undefined : editing.id, input })
                toast.success('QA test saved')
                setEditing(null)
              }}
            />
          )}
        </DialogContent>
      </Dialog>
    </>
  )
}

function toInput(t: QaTest): QaTestInput {
  return {
    code: t.code,
    name: t.name,
    unit: t.unit ?? undefined,
    lowerLimit: t.lowerLimit ?? undefined,
    upperLimit: t.upperLimit ?? undefined,
    reference: t.reference ?? undefined,
    placeholder: t.placeholder,
    active: t.active,
  }
}

const FIELDS = ['code', 'name', 'unit', 'lowerLimit', 'upperLimit', 'reference'] as const

function QaTestForm({
  test,
  onSubmit,
  onCancel,
}: {
  test?: QaTest
  onSubmit: (input: QaTestInput) => Promise<void>
  onCancel: () => void
}) {
  const [formErrors, setFormErrors] = useState<string[]>([])
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<QaTestValues>({
    resolver: zodResolver(qaTestSchema),
    defaultValues: {
      code: test?.code ?? '',
      name: test?.name ?? '',
      unit: test?.unit ?? '',
      lowerLimit: test?.lowerLimit != null ? String(test.lowerLimit) : '',
      upperLimit: test?.upperLimit != null ? String(test.upperLimit) : '',
      reference: test?.reference ?? '',
      placeholder: test?.placeholder ?? false,
    },
  })

  const submit = handleSubmit(async (v) => {
    setFormErrors([])
    try {
      await onSubmit(toQaTestInput(v, test?.active ?? true))
    } catch (err) {
      const apiError = toApiError(err)
      if (apiError.status === 409) {
        setError('code', { type: 'server', message: apiError.message })
        return
      }
      setFormErrors(applyFieldErrors(apiError, setError, FIELDS))
    }
  })

  return (
    <form onSubmit={submit} noValidate className="space-y-3">
      {formErrors.length > 0 && (
        <div
          role="alert"
          className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
        >
          {formErrors.map((m) => (
            <p key={m}>{m}</p>
          ))}
        </div>
      )}
      <div className="grid gap-3 sm:grid-cols-2">
        <Input label="Code" error={errors.code?.message} {...register('code')} />
        <Input label="Unit" error={errors.unit?.message} {...register('unit')} />
      </div>
      <Input label="Name" error={errors.name?.message} {...register('name')} />
      <div className="grid gap-3 sm:grid-cols-2">
        <Input
          label="Lower limit"
          inputMode="decimal"
          hint="Empty = no lower bound"
          error={errors.lowerLimit?.message}
          {...register('lowerLimit')}
        />
        <Input
          label="Upper limit"
          inputMode="decimal"
          hint="Empty = no upper bound"
          error={errors.upperLimit?.message}
          {...register('upperLimit')}
        />
      </div>
      <Input
        label="Reference"
        hint="Where the tolerance comes from (protocol, physicist sign-off)"
        {...register('reference')}
      />
      <label className="flex items-center gap-2 text-sm">
        <input type="checkbox" {...register('placeholder')} />
        Placeholder tolerance (to be confirmed by a medical physicist)
      </label>
      <div className="flex justify-end gap-2 pt-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          Save
        </Button>
      </div>
    </form>
  )
}
