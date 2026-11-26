import { screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it } from 'vitest'
import { API, server } from '@/test/server'
import { loginAs, renderApp } from '@/test/utils'

const PATIENT_ID = '66666666-6666-4666-8666-666666666666'
const page = <T,>(content: T[]) => ({ content, page: 0, size: 50, totalElements: content.length, totalPages: 1 })
const appt = (id: string, status: string) => ({
  id, patientId: PATIENT_ID, patientName: 'TEST Patient', staffId: 's1', staffName: 'TEST Neuro',
  scheduledTime: '2026-12-01T09:00:00Z', durationMinutes: 30, status, reason: null,
})

function mockApi(onCreate: (body: Record<string, unknown>) => void) {
  server.use(
    http.get(`${API}/api/mri-orders`, () => HttpResponse.json(page([]))),
    http.get(`${API}/api/patients`, () =>
      HttpResponse.json(page([{ id: PATIENT_ID, name: 'TEST Patient', patientNumber: 'NG-09999' }])),
    ),
    http.get(`${API}/api/patients/${PATIENT_ID}/appointments`, () =>
      HttpResponse.json(page([appt('a-scheduled', 'SCHEDULED'), appt('a-completed', 'COMPLETED'), appt('a-cancelled', 'CANCELLED'), appt('a-missed', 'MISSED')])),
    ),
    http.post(`${API}/api/mri-orders`, async ({ request }) => {
      const body = (await request.json()) as Record<string, unknown>
      onCreate(body)
      return HttpResponse.json({
        id: 'o1', appointmentId: body.appointmentId, patientId: PATIENT_ID, patientName: 'TEST Patient',
        accessionNumber: 'ACC-2026-001000', indication: body.indication ?? null, status: 'ORDERED',
      }, { status: 201 })
    }),
  )
}

describe('MRI order form', () => {
  it('has no accession input, hides cancelled and missed appointments and shows the generated number', async () => {
    let sent: Record<string, unknown> | undefined
    mockApi((b) => { sent = b })
    loginAs({ role: 'CLINICAL_STAFF', scope: 'NEUROLOGIST' })
    renderApp('/mri-orders')
    const user = userEvent.setup()

    await screen.findByRole('option', { name: 'TEST Patient — NG-09999' })
    await user.selectOptions(screen.getByRole('combobox', { name: /^Patient/ }), PATIENT_ID)
    const apptSelect = screen.getByRole('combobox', { name: /Appointment/ })
    await waitFor(() => expect(apptSelect.querySelectorAll('option')).toHaveLength(3))
    expect(apptSelect.querySelector('option[value="a-cancelled"]')).toBeNull()
    expect(apptSelect.querySelector('option[value="a-missed"]')).toBeNull()
    expect(apptSelect.querySelector('option[value="a-completed"]')).not.toBeNull()
    expect(screen.queryByText(/Accession number/)).not.toBeInTheDocument()

    await user.selectOptions(apptSelect, 'a-scheduled')
    await user.click(screen.getByRole('button', { name: 'Create order' }))

    expect(await screen.findByText('MRI order ACC-2026-001000 created.')).toBeInTheDocument()
    expect(sent).toEqual({ appointmentId: 'a-scheduled', indication: '' })
  })
})
