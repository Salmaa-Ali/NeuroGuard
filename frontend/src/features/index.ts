import { admin } from './admin-staff/feature'
import { appointments } from './appointments/feature'
import { audit } from './audit/feature'
import { billing } from './billing/feature'
import { cdss } from './cdss/feature'
import { clinical } from './clinical/feature'
import { imaging } from './imaging/feature'
import { patients } from './patients/feature'
import { reports } from './reports/feature'
import { scanners } from './scanners/feature'
import type { Feature } from './types'

/**
 * Order here is the order of the sidebar. Add new features to this list.
 * (The dashboard is shell-level and registered in routes.tsx.)
 */
export const features: Feature[] = [
  admin,
  audit,
  patients,
  appointments,
  imaging,
  clinical,
  billing,
  scanners,
  reports,
  cdss,
]
