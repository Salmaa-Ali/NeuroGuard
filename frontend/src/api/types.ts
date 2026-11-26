/**
 * Domain types derived from the generated OpenAPI types (schema.d.ts, regenerate with `npm run gen:api`).
 *
 * The backend does not mark response fields as required, so the generator makes them all optional.
 * These helpers make them required again and mark the fields the API really can send as null.
 * If the backend contract changes, the derivation below stops compiling and shows what to fix.
 */
import type { components } from './schema'

type S = components['schemas']

type NullableFields<T, K extends keyof T> = Omit<Required<T>, K> & {
  [P in K]-?: Required<T>[P] | null
}

export type UserType = NonNullable<S['LoginResponse']['userType']>
export type Role = NonNullable<S['LoginResponse']['role']>
export type BaseRole = S['CreateStaffRequest']['baseRole']
export type PermissionScope = NonNullable<S['CreateStaffRequest']['permissionScope']>

export type LoginResult = NullableFields<S['LoginResponse'], 'scope'>
export type Me = NullableFields<S['MeResponse'], 'scope'>
export type StaffMember = NullableFields<S['StaffResponse'], 'permissionScope'>
export type PatientSummary = NullableFields<S['PatientSummary'], 'phone' | 'neurologyHistory'>
export type StaffPage = Omit<Required<S['PageResponseStaffResponse']>, 'content'> & {
  content: StaffMember[]
}

export type LoginInput = S['LoginRequest']
export type CreateStaffInput = Omit<S['CreateStaffRequest'], 'permissionScope'> & {
  permissionScope: PermissionScope | null
}
export type ChangeRoleInput = Omit<S['ChangeStaffRoleRequest'], 'permissionScope'> & {
  permissionScope: PermissionScope | null
}

/** Backend only supports ascending sort on these columns. */
export type StaffSortBy = 'name' | 'email' | 'createdAt'

export const BASE_ROLES: BaseRole[] = ['ADMIN', 'CLINICAL_STAFF']
export const PERMISSION_SCOPES: PermissionScope[] = [
  'NEUROLOGIST',
  'RADIOLOGIST',
  'MRI_TECHNOLOGIST',
  'BIOMEDICAL_ENGINEER',
  'BILLING_CLERK',
]

// ---- Scanners & QA ----

export type ScannerStatus = NonNullable<S['ScannerListItem']['status']>
export type QaStatus = NonNullable<S['ScannerListItem']['qaStatus']>
export type CalibrationResult = NonNullable<S['CalibrationSummary']['result']>

export type ScannerListItem = NullableFields<
  S['ScannerListItem'],
  'stationName' | 'lastQaAt' | 'lastQaResult' | 'nextDueDate' | 'riskScore'
>
export type ScannerPage = Omit<Required<S['PageResponseScannerListItem']>, 'content'> & {
  content: ScannerListItem[]
}
export type Accessory = NullableFields<S['AccessoryResponse'], 'type' | 'serialNumber'>
export type CalibrationSummary = NullableFields<S['CalibrationSummary'], 'certificateRef'>
export type CalibrationPage = Omit<Required<S['PageResponseCalibrationSummary']>, 'content'> & {
  content: CalibrationSummary[]
}
export type ScannerDetail = Omit<
  NullableFields<S['ScannerDetail'], 'stationName' | 'location' | 'installDate'>,
  'latestSession' | 'accessories'
> & {
  latestSession: CalibrationSummary | null
  accessories: Accessory[]
}
export type MeasurementRow = NullableFields<
  S['MeasurementResponse'],
  'unit' | 'lowerLimit' | 'upperLimit'
>
export type CalibrationDetail = Omit<
  NullableFields<S['CalibrationDetail'], 'notes' | 'certificateRef'>,
  'measurements'
> & { measurements: MeasurementRow[] }
export type QaTest = NullableFields<
  S['QaTestResponse'],
  'unit' | 'lowerLimit' | 'upperLimit' | 'reference'
>
export type QaSummary = NullableFields<
  S['QaSummary'],
  | 'lastQaAt'
  | 'lastQaResult'
  | 'nextDueDate'
  | 'riskScore'
  | 'sourceSessionId'
  | 'sourceSessionRecordedAt'
>
export type QaOverview = Required<S['QaOverview']>

export type ScannerInput = S['ScannerRequest']
export type AccessoryInput = S['AccessoryRequest']
export type QaTestInput = S['QaTestRequest']
export type RecordCalibrationInput = S['RecordCalibrationRequest']

export const SCANNER_STATUSES: ScannerStatus[] = ['ACTIVE', 'OUT_OF_SERVICE', 'RETIRED']
export const QA_STATUSES: QaStatus[] = ['OK', 'DUE_SOON', 'OVERDUE', 'FAILED', 'NEVER_CALIBRATED']
