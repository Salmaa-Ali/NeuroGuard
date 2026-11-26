import { zodResolver } from '@hookform/resolvers/zod'
import { useMemo, useState } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { z } from 'zod'
import { toApiError } from '@/api/errors'
import type { QaTest, RecordCalibrationInput } from '@/api/types'
import { Input } from '@/components/Input'
import { Button } from '@/components/ui/button'
import { cn } from '@/lib/utils'
import { PLACEHOLDER_TEXT, formatLimits, isWithin, toLocalInput } from './qa'

function buildSchema(now: () => Date) {
  return z
    .object({
      performedAt: z.string().min(1, 'Date and time are required'),
      notes: z.string(),
      certificateRef: z.string().max(100, 'At most 100 characters'),
      values: z.record(z.string(), z.string()),
    })
    .superRefine((v, ctx) => {
      const when = new Date(v.performedAt)
      if (v.performedAt && Number.isNaN(when.getTime())) {
        ctx.addIssue({
          code: 'custom',
          path: ['performedAt'],
          message: 'Enter a valid date and time',
        })
      } else if (v.performedAt && when.getTime() > now().getTime()) {
        ctx.addIssue({
          code: 'custom',
          path: ['performedAt'],
          message: 'Date must not be in the future',
        })
      }
      const entered = Object.entries(v.values).filter(([, value]) => value.trim() !== '')
      if (entered.length === 0) {
        ctx.addIssue({
          code: 'custom',
          path: ['values'],
          message: 'Enter at least one measured value',
        })
      }
      for (const [id, value] of entered) {
        if (!Number.isFinite(Number(value))) {
          ctx.addIssue({ code: 'custom', path: ['values', id], message: 'Enter a number' })
        }
      }
    })
}
type Values = z.infer<ReturnType<typeof buildSchema>>

interface CalibrationFormProps {
  /** Active QA tests, one row each. */
  tests: QaTest[]
  /** Should throw (the axios error) when the backend rejects it. */
  onSubmit: (input: RecordCalibrationInput) => Promise<void>
  onCancel: () => void
  /** For tests. */
  now?: () => Date
}

/**
 * Enter the measured values of a QA session. The tolerance hint is only a preview: the server computes and
 * saves the real result (PASS only if every value is within its limits).
 */
export function CalibrationForm({
  tests,
  onSubmit,
  onCancel,
  now = () => new Date(),
}: CalibrationFormProps) {
  const schema = useMemo(() => buildSchema(now), [now])
  const [formError, setFormError] = useState<string | null>(null)
  const {
    register,
    handleSubmit,
    control,
    formState: { errors, isSubmitting },
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: {
      performedAt: toLocalInput(now()),
      notes: '',
      certificateRef: '',
      values: Object.fromEntries(tests.map((t) => [t.id, ''])),
    },
  })
  const values = useWatch({ control, name: 'values' })

  const submit = handleSubmit(async (v) => {
    setFormError(null)
    try {
      await onSubmit({
        performedAt: new Date(v.performedAt).toISOString(),
        notes: v.notes.trim() || undefined,
        certificateRef: v.certificateRef.trim() || undefined,
        measurements: Object.entries(v.values)
          .filter(([, value]) => value.trim() !== '')
          .map(([qaTestId, value]) => ({ qaTestId, measuredValue: Number(value) })),
      })
    } catch (err) {
      const e = toApiError(err)
      setFormError([e.message, ...e.fieldErrors.map((f) => `${f.field}: ${f.message}`)].join(' · '))
    }
  })

  const valuesError = (errors.values as { message?: string } | undefined)?.message

  return (
    <form onSubmit={submit} noValidate className="space-y-5">
      {formError && (
        <div
          role="alert"
          className="rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-800"
        >
          {formError}
        </div>
      )}
      <div className="grid gap-3 sm:grid-cols-3">
        <Input
          label="Performed at"
          type="datetime-local"
          error={errors.performedAt?.message}
          {...register('performedAt')}
        />
        <Input
          label="Certificate ref"
          error={errors.certificateRef?.message}
          {...register('certificateRef')}
        />
        <Input label="Notes" {...register('notes')} />
      </div>

      <div className="overflow-x-auto rounded-lg border border-border">
        <table className="w-full text-sm">
          <thead className="bg-muted/40 text-left text-[11px] uppercase tracking-wider text-muted-foreground">
            <tr>
              <th className="px-4 py-2">QA test</th>
              <th className="px-4 py-2">Tolerance</th>
              <th className="px-4 py-2">Measured value</th>
              <th className="px-4 py-2">Preview</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-border">
            {tests.map((t) => {
              const raw = values?.[t.id] ?? ''
              const num = Number(raw)
              const hasValue = raw.trim() !== '' && Number.isFinite(num)
              const within = hasValue ? isWithin(num, t.lowerLimit, t.upperLimit) : null
              const rowError = (
                errors.values as Record<string, { message?: string }> | undefined
              )?.[t.id]?.message
              return (
                <tr key={t.id}>
                  <td className="px-4 py-2">
                    <p className="font-medium">{t.name}</p>
                    <p className="font-mono text-xs text-muted-foreground">{t.code}</p>
                  </td>
                  <td className="px-4 py-2">
                    {formatLimits(t.lowerLimit, t.upperLimit, t.unit)}
                    {t.placeholder && (
                      <p className="text-xs text-warning-foreground">{PLACEHOLDER_TEXT}</p>
                    )}
                  </td>
                  <td className="w-44 px-4 py-2">
                    <Input
                      label={`Value for ${t.name}`}
                      inputMode="decimal"
                      error={rowError}
                      {...register(`values.${t.id}`)}
                    />
                  </td>
                  <td className="px-4 py-2" aria-live="polite">
                    {within === null ? (
                      <span className="text-muted-foreground">—</span>
                    ) : (
                      <span
                        data-testid={`hint-${t.code}`}
                        className={cn(
                          'text-xs font-medium',
                          within ? 'text-success' : 'text-destructive',
                        )}
                      >
                        {within ? 'Within tolerance' : 'Out of tolerance'}
                      </span>
                    )}
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>
      {valuesError && (
        <p role="alert" className="text-sm text-red-600">
          {valuesError}
        </p>
      )}
      <p className="text-xs text-muted-foreground">
        Leave a row empty if that test was not performed. The preview is a hint only; the server
        decides and saves the result.
      </p>

      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          Save session
        </Button>
      </div>
    </form>
  )
}
