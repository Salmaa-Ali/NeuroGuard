import { z } from 'zod'
import type { QaTestInput, ScannerInput } from '@/api/types'

const optionalText = (max: number) => z.string().trim().max(max, `At most ${max} characters`)

/** Empty string -> null, otherwise a finite number. Used for optional numeric inputs. */
const optionalNumber = z
  .string()
  .trim()
  .refine((v) => v === '' || Number.isFinite(Number(v)), 'Enter a number')

export const scannerSchema = z.object({
  name: z.string().trim().min(1, 'Name is required').max(255),
  manufacturer: z.string().trim().min(1, 'Manufacturer is required').max(255),
  model: z.string().trim().min(1, 'Model is required').max(255),
  serialNumber: z.string().trim().min(1, 'Serial number is required').max(100),
  stationName: optionalText(255),
  fieldStrength: z
    .string()
    .trim()
    .refine((v) => Number(v) > 0, 'Field strength must be greater than 0'),
  location: optionalText(255),
  installDate: z.string(),
  calibrationIntervalDays: z
    .string()
    .trim()
    .refine(
      (v) => Number.isInteger(Number(v)) && Number(v) > 0,
      'Interval must be a whole number of days > 0',
    ),
})
export type ScannerValues = z.infer<typeof scannerSchema>

export function toScannerInput(v: ScannerValues): ScannerInput {
  return {
    name: v.name,
    manufacturer: v.manufacturer,
    model: v.model,
    serialNumber: v.serialNumber,
    stationName: v.stationName || undefined,
    fieldStrength: Number(v.fieldStrength),
    location: v.location || undefined,
    installDate: v.installDate || undefined,
    calibrationIntervalDays: Number(v.calibrationIntervalDays),
  }
}

export const qaTestSchema = z
  .object({
    code: z.string().trim().min(1, 'Code is required').max(50),
    name: z.string().trim().min(1, 'Name is required').max(255),
    unit: optionalText(30),
    lowerLimit: optionalNumber,
    upperLimit: optionalNumber,
    reference: z.string(),
    placeholder: z.boolean(),
  })
  .superRefine((v, ctx) => {
    if (v.lowerLimit !== '' && v.upperLimit !== '' && Number(v.lowerLimit) > Number(v.upperLimit)) {
      ctx.addIssue({
        code: 'custom',
        path: ['lowerLimit'],
        message: 'Lower limit must not exceed upper limit',
      })
    }
  })
export type QaTestValues = z.infer<typeof qaTestSchema>

export function toQaTestInput(v: QaTestValues, active: boolean): QaTestInput {
  return {
    code: v.code,
    name: v.name,
    unit: v.unit || undefined,
    lowerLimit: v.lowerLimit === '' ? undefined : Number(v.lowerLimit),
    upperLimit: v.upperLimit === '' ? undefined : Number(v.upperLimit),
    reference: v.reference.trim() || undefined,
    placeholder: v.placeholder,
    active,
  }
}
