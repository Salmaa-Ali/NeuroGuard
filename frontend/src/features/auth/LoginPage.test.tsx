import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { clearSession } from '../../auth/session'
import { API, apiError, defaultMe, server } from '../../test/server'
import { loginResponse, renderApp } from '../../test/utils'

async function fillAndSubmit(email: string, password: string) {
  const user = userEvent.setup()
  await user.type(screen.getByLabelText('Email'), email)
  await user.type(screen.getByLabelText('Password'), password)
  await user.click(screen.getByRole('button', { name: 'Log in' }))
  return user
}

describe('LoginPage', () => {
  it('logs a staff member in and shows the dashboard with their name', async () => {
    server.use(
      http.post(`${API}/api/auth/staff/login`, () => HttpResponse.json(loginResponse('ADMIN'))),
      http.get(`${API}/api/me`, () => HttpResponse.json({ ...defaultMe, name: 'Ada Admin' })),
    )
    renderApp('/login')
    await fillAndSubmit('admin@example.com', 'secret-pass')

    expect(await screen.findByRole('heading', { name: /Welcome, Ada Admin/ })).toBeInTheDocument()
    expect(screen.getByRole('navigation', { name: 'Main' })).toHaveTextContent('Staff management')
    // the top bar shows the name, not the email
    expect(await screen.findByRole('banner')).toHaveTextContent('Ada Admin')
    expect(screen.getByRole('banner')).not.toHaveTextContent('admin@example.com')
  })

  it('uses the patient endpoint when "Patient" is selected', async () => {
    const patientLogin = vi.fn()
    const staffLogin = vi.fn()
    server.use(
      http.post(`${API}/api/auth/patient/login`, async ({ request }) => {
        patientLogin(await request.json())
        return HttpResponse.json(loginResponse('PATIENT'))
      }),
      http.post(`${API}/api/auth/staff/login`, () => {
        staffLogin()
        return apiError(401, 'Invalid email or password')
      }),
    )
    renderApp('/login')
    await userEvent.setup().click(screen.getByLabelText('Patient'))
    await fillAndSubmit('pat@example.com', 'secret-pass')

    expect(await screen.findByRole('heading', { name: /Welcome/ })).toBeInTheDocument()
    expect(patientLogin).toHaveBeenCalledWith({ email: 'pat@example.com', password: 'secret-pass' })
    expect(staffLogin).not.toHaveBeenCalled()
  })

  it("shows the backend's message on a 401 and does not treat it as an expired session", async () => {
    server.use(
      http.post(`${API}/api/auth/staff/login`, () => apiError(401, 'Invalid email or password')),
    )
    renderApp('/login')
    await fillAndSubmit('admin@example.com', 'wrong-password')

    expect(await screen.findByRole('alert')).toHaveTextContent('Invalid email or password')
    expect(screen.queryByText(/Session expired/)).not.toBeInTheDocument()
    expect(sessionStorage.getItem('neuroguard.session')).toBeNull()
  })

  it('maps backend field errors onto the matching fields', async () => {
    server.use(
      http.post(`${API}/api/auth/staff/login`, () =>
        apiError(400, 'Validation failed', [{ field: 'email', message: 'must be a valid email' }]),
      ),
    )
    renderApp('/login')
    await fillAndSubmit('someone@example.com', 'secret-pass')

    const email = screen.getByLabelText('Email')
    await vi.waitFor(() => expect(email).toHaveAccessibleDescription('must be a valid email'))
    expect(email).toBeInvalid()
  })

  it('validates required fields before calling the backend', async () => {
    renderApp('/login')
    await userEvent.setup().click(screen.getByRole('button', { name: 'Log in' }))

    expect(await screen.findByText('Email is required')).toBeInTheDocument()
    expect(screen.getByText('Password is required')).toBeInTheDocument()
  })

  it('tells the user when the session expired', () => {
    clearSession('expired')
    renderApp('/login')
    expect(screen.getByRole('status')).toHaveTextContent('Session expired. Please log in again.')
  })

  it('shows a friendly message when the server cannot be reached', async () => {
    server.use(http.post(`${API}/api/auth/staff/login`, () => HttpResponse.error()))
    renderApp('/login')
    await fillAndSubmit('admin@example.com', 'secret-pass')
    expect(await screen.findByRole('alert')).toHaveTextContent('Cannot reach the server')
  })
})
