import { screen } from '@testing-library/react'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import type { PermissionScope } from '@/api/types'
import { API, server } from '@/test/server'
import { loginAs, renderApp } from '@/test/utils'

const SCANNER_ID = '33333333-3333-4333-8333-333333333333'
const DENIED = "You don't have access to this page"

const listItem = {
  id: SCANNER_ID,
  name: 'TEST MRI-1 (1.5 T)',
  manufacturer: 'TEST Mfr',
  model: 'TEST Model',
  fieldStrength: 1.5,
  stationName: 'TEST Station',
  status: 'ACTIVE',
  qaStatus: 'OVERDUE',
  lastQaAt: '2026-01-10T09:00:00Z',
  lastQaResult: 'PASS',
  nextDueDate: '2026-04-10',
  riskScore: 24.2,
}

const session = {
  id: '44444444-4444-4444-8444-444444444444',
  scannerId: SCANNER_ID,
  performedAt: '2026-01-10T09:00:00Z',
  performedById: '55555555-5555-4555-8555-555555555555',
  performedByName: 'TEST Engineer',
  result: 'PASS',
  nextDueDate: '2026-04-10',
  riskScore: 24.2,
  certificateRef: null,
  recordedAt: '2026-01-10T09:05:00Z',
}

function mockScannerApi() {
  server.use(
    http.get(`${API}/api/scanners`, () =>
      HttpResponse.json({
        content: [listItem],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }),
    ),
    http.get(`${API}/api/scanners/${SCANNER_ID}`, () =>
      HttpResponse.json({
        ...listItem,
        serialNumber: 'TEST-SN-1',
        location: null,
        installDate: null,
        calibrationIntervalDays: 90,
        latestSession: session,
        accessories: [{ id: 'c1', name: 'TEST Head coil', type: 'Head', serialNumber: null }],
        createdAt: '2025-01-01T00:00:00Z',
        updatedAt: '2025-01-01T00:00:00Z',
      }),
    ),
    http.get(`${API}/api/scanners/${SCANNER_ID}/qa-summary`, () =>
      HttpResponse.json({
        scannerId: SCANNER_ID,
        scannerName: listItem.name,
        fieldStrength: 1.5,
        qaStatus: 'OVERDUE',
        flaggedForReview: true,
        lastQaAt: session.performedAt,
        lastQaResult: 'PASS',
        nextDueDate: '2026-04-10',
        riskScore: 24.2,
        sourceSessionId: session.id,
        sourceSessionRecordedAt: session.recordedAt,
        evaluatedOn: '2026-06-01',
      }),
    ),
    http.get(`${API}/api/scanners/${SCANNER_ID}/calibrations`, () =>
      HttpResponse.json({ content: [session], page: 0, size: 20, totalElements: 1, totalPages: 1 }),
    ),
  )
}

const clinical = (scope: PermissionScope) => loginAs({ role: 'CLINICAL_STAFF', scope })

describe('scanner pages by role', () => {
  it('the biomedical engineer sees the write buttons', async () => {
    mockScannerApi()
    clinical('BIOMEDICAL_ENGINEER')
    renderApp('/scanners')
    expect(await screen.findByText('TEST MRI-1 (1.5 T)')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Add scanner/ })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'QA tests' })).toBeInTheDocument()
    expect(screen.getAllByText(/Flagged for review/).length).toBeGreaterThan(0)
    expect(screen.getByText('Risk score (indicative)')).toBeInTheDocument()
  })

  it.each(['NEUROLOGIST', 'RADIOLOGIST', 'MRI_TECHNOLOGIST'] as const)(
    '%s can read the list and the detail page without write buttons',
    async (scope) => {
      mockScannerApi()
      clinical(scope)
      const { router } = renderApp('/scanners')
      expect(await screen.findByText('TEST MRI-1 (1.5 T)')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /Add scanner/ })).not.toBeInTheDocument()

      await router.navigate(`/scanners/${SCANNER_ID}`)
      expect(await screen.findByText('TEST Head coil')).toBeInTheDocument()
      expect(await screen.findByLabelText('Scanner QA status')).toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /Edit scanner/ })).not.toBeInTheDocument()
      expect(screen.queryByRole('link', { name: /Record calibration/ })).not.toBeInTheDocument()
      expect(screen.queryByRole('button', { name: /Add coil/ })).not.toBeInTheDocument()
      expect(screen.queryByLabelText('Scanner status')).not.toBeInTheDocument()
    },
  )

  it('admin can read without write buttons', async () => {
    mockScannerApi()
    loginAs({ role: 'ADMIN' })
    renderApp(`/scanners/${SCANNER_ID}`)
    expect(await screen.findByText('TEST Head coil')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /Edit scanner/ })).not.toBeInTheDocument()
  })

  it('the biomedical engineer sees write buttons on the detail page', async () => {
    mockScannerApi()
    clinical('BIOMEDICAL_ENGINEER')
    renderApp(`/scanners/${SCANNER_ID}`)
    expect(await screen.findByRole('button', { name: /Edit scanner/ })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /Record calibration/ })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Add coil/ })).toBeInTheDocument()
  })

  it('billing clerk and patient get the 403 screen', async () => {
    clinical('BILLING_CLERK')
    const first = renderApp('/scanners')
    expect(await screen.findByRole('heading', { name: DENIED })).toBeInTheDocument()
    first.unmount()

    loginAs({ role: 'PATIENT' })
    renderApp(`/scanners/${SCANNER_ID}`)
    expect(await screen.findByRole('heading', { name: DENIED })).toBeInTheDocument()
  })

  it('read-only roles cannot open the record or QA test pages', async () => {
    clinical('NEUROLOGIST')
    const first = renderApp(`/scanners/${SCANNER_ID}/calibrations/new`)
    expect(await screen.findByRole('heading', { name: DENIED })).toBeInTheDocument()
    first.unmount()
    loginAs({ role: 'ADMIN' })
    renderApp('/scanners/qa-tests')
    expect(await screen.findByRole('heading', { name: DENIED })).toBeInTheDocument()
  })

  it('a backend 403 on the list shows the no-access screen', async () => {
    server.use(
      http.get(`${API}/api/scanners`, () =>
        HttpResponse.json({ status: 403, message: 'Forbidden', fieldErrors: [] }, { status: 403 }),
      ),
    )
    clinical('NEUROLOGIST')
    renderApp('/scanners')
    expect(await screen.findByRole('heading', { name: DENIED })).toBeInTheDocument()
  })
})
