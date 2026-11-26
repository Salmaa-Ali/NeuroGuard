import { zodResolver } from '@hookform/resolvers/zod'
import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { applyFieldErrors, toApiError } from '@/api/errors'
import type { ScannerDetail, ScannerInput } from '@/api/types'
import { Input } from '@/components/Input'
import { Button } from '@/components/ui/button'
import { scannerSchema, toScannerInput, type ScannerValues } from './schema'

const FIELDS = [
  'name',
  'manufacturer',
  'model',
  'serialNumber',
  'stationName',
  'fieldStrength',
  'location',
  'installDate',
  'calibrationIntervalDays',
] as const

interface ScannerFormProps {
  initial?: ScannerDetail
  submitLabel: string
  /** Should throw (the axios error) when the backend rejects the request. */
  onSubmit: (input: ScannerInput) => Promise<void>
  onCancel: () => void
}

export function ScannerForm({ initial, submitLabel, onSubmit, onCancel }: ScannerFormProps) {
  const [formErrors, setFormErrors] = useState<string[]>([])
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting },
  } = useForm<ScannerValues>({
    resolver: zodResolver(scannerSchema),
    defaultValues: {
      name: initial?.name ?? '',
      manufacturer: initial?.manufacturer ?? '',
      model: initial?.model ?? '',
      serialNumber: initial?.serialNumber ?? '',
      stationName: initial?.stationName ?? '',
      fieldStrength: initial ? String(initial.fieldStrength) : '',
      location: initial?.location ?? '',
      installDate: initial?.installDate ?? '',
      calibrationIntervalDays: initial ? String(initial.calibrationIntervalDays) : '',
    },
  })

  const submit = handleSubmit(async (values) => {
    setFormErrors([])
    try {
      await onSubmit(toScannerInput(values))
    } catch (err) {
      const apiError = toApiError(err)
      if (apiError.status === 409) {
        setError('serialNumber', { type: 'server', message: apiError.message })
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
      <Input label="Name" error={errors.name?.message} {...register('name')} />
      <div className="grid gap-3 sm:grid-cols-2">
        <Input
          label="Manufacturer"
          error={errors.manufacturer?.message}
          {...register('manufacturer')}
        />
        <Input label="Model" error={errors.model?.message} {...register('model')} />
        <Input
          label="Serial number"
          error={errors.serialNumber?.message}
          {...register('serialNumber')}
        />
        <Input
          label="Field strength (T)"
          inputMode="decimal"
          error={errors.fieldStrength?.message}
          {...register('fieldStrength')}
        />
        <Input
          label="Station name"
          error={errors.stationName?.message}
          {...register('stationName')}
        />
        <Input label="Location" error={errors.location?.message} {...register('location')} />
        <Input
          label="Install date"
          type="date"
          error={errors.installDate?.message}
          {...register('installDate')}
        />
        <Input
          label="QA interval (days)"
          inputMode="numeric"
          error={errors.calibrationIntervalDays?.message}
          {...register('calibrationIntervalDays')}
        />
      </div>
      <div className="flex justify-end gap-2 pt-2">
        <Button type="button" variant="outline" onClick={onCancel}>
          Cancel
        </Button>
        <Button type="submit" disabled={isSubmitting}>
          {submitLabel}
        </Button>
      </div>
    </form>
  )
}
