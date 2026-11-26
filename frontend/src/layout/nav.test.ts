import { describe, expect, it } from 'vitest'
import type { PermissionScope } from '../api/types'
import { getNavItems } from './nav'

const labels = (user: Parameters<typeof getNavItems>[0]) => getNavItems(user).map((i) => i.label)
const clinical = (scope: PermissionScope) => ({ role: 'CLINICAL_STAFF' as const, scope })

/** Guards the role -> menu mapping (see docs/permissions.md and the per-role menus in the brief). */
describe('role-based navigation', () => {
  it('admin', () => {
    expect(labels({ role: 'ADMIN', scope: null })).toEqual([
      'Dashboard',
      'Staff management',
      'Audit log',
      'Scanners & QA',
      'Reports',
    ])
  })

  it('neurologist', () => {
    expect(labels(clinical('NEUROLOGIST'))).toEqual([
      'Dashboard',
      'Patients',
      'Appointments',
      'MRI orders',
      'Clinical',
    ])
  })

  it('radiologist', () => {
    expect(labels(clinical('RADIOLOGIST'))).toEqual([
      'Dashboard',
      'Patients',
      'MRI studies / Viewer',
      'Reports',
    ])
  })

  it('MRI technologist', () => {
    expect(labels(clinical('MRI_TECHNOLOGIST'))).toEqual([
      'Dashboard',
      'MRI orders',
      'DICOM upload',
    ])
  })

  it('biomedical engineer', () => {
    expect(labels(clinical('BIOMEDICAL_ENGINEER'))).toEqual(['Dashboard', 'Scanners & QA'])
  })

  it('billing clerk', () => {
    expect(labels(clinical('BILLING_CLERK'))).toEqual([
      'Dashboard',
      'Patients',
      'Invoices',
      'Payments',
    ])
  })

  it('patient', () => {
    expect(labels({ role: 'PATIENT', scope: null })).toEqual([
      'Dashboard',
      'My profile',
      'My appointments',
      'My prescriptions',
      'My invoices',
      'My reports',
    ])
  })
})
