import { screen, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { describe, expect, it, vi } from 'vitest'
import { API, apiError, defaultMe, server } from './test/server'
import { loginAs, renderApp, TEST_USER_ID } from './test/utils'

const DENIED = "You don't have access to this page"

const staffMember = (over: Record<string, unknown> = {}) => ({
  id: '22222222-2222-4222-8222-222222222222',
  name: 'Nora Neuro',
  email: 'nora@example.com',
  baseRole: 'CLINICAL_STAFF',
  permissionScope: 'NEUROLOGIST',
  createdAt: '2026-11-21T10:00:00Z',
  updatedAt: '2026-11-21T10:00:00Z',
  ...over,
})

const staffPage = (content = [staffMember()], over: Record<string, unknown> = {}) => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
  ...over,
})

describe('access control in the app', () => {
  it('lets a patient open My profile but shows the 403 screen on /admin/staff', async () => {
    const staffCalls = vi.fn()
    let profileAuth: string | null = null
    server.use(
      http.get(`${API}/api/admin/staff`, () => {
        staffCalls()
        return HttpResponse.json(staffPage())
      }),
      http.get(`${API}/api/patients/${TEST_USER_ID}`, ({ request }) => {
        profileAuth = request.headers.get('authorization')
        return HttpResponse.json({
          id: TEST_USER_ID,
          patientNumber: 'P-0001',
          name: 'Pat Patient',
          email: 'pat@example.com',
          dateOfBirth: '1990-01-01',
          phone: null,
          neurologyHistory: null,
        })
      }),
    )

    loginAs({ role: 'PATIENT' })
    const { router, unmount } = renderApp('/profile')
    expect(await screen.findByText('Pat Patient')).toBeInTheDocument()
    expect(profileAuth).toMatch(/^Bearer /)
    unmount()

    loginAs({ role: 'PATIENT' })
    renderApp('/admin/staff')
    expect(await screen.findByRole('heading', { name: DENIED })).toBeInTheDocument()
    expect(staffCalls).not.toHaveBeenCalled() // blocked in the UI, no request made
    expect(router).toBeDefined()
  })

  it('shows the 403 screen (without logging out) when the backend answers 403', async () => {
    server.use(
      http.get(`${API}/api/patients/:id`, () => apiError(403, 'You do not have permission')),
    )
    loginAs({ role: 'CLINICAL_STAFF', scope: 'NEUROLOGIST' })
    renderApp('/patients/33333333-3333-4333-8333-333333333333')

    expect(await screen.findByRole('heading', { name: DENIED })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Log out' })).toBeInTheDocument()
  })

  it('shows the 404 page for unknown routes', async () => {
    loginAs({ role: 'ADMIN' })
    renderApp('/does-not-exist')
    expect(await screen.findByRole('heading', { name: 'Page not found' })).toBeInTheDocument()
  })

  it('has an empty viewer container for the DICOM team', async () => {
    loginAs({ role: 'CLINICAL_STAFF', scope: 'RADIOLOGIST' })
    renderApp('/studies/1.2.3/viewer')
    const container = await screen.findByTestId('viewer-container')
    expect(container).toBeEmptyDOMElement()
  })

  it('blocks the viewer for roles without access', async () => {
    loginAs({ role: 'CLINICAL_STAFF', scope: 'BILLING_CLERK' })
    renderApp('/studies/1.2.3/viewer')
    expect(await screen.findByRole('heading', { name: DENIED })).toBeInTheDocument()
  })
})

describe('session expiry', () => {
  it('sends an already expired token back to login with "Session expired"', async () => {
    loginAs({ role: 'ADMIN', ttlMs: -1000 })
    renderApp('/')
    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Session expired')
  })

  it('logs out automatically when the token expires while the app is open', async () => {
    loginAs({ role: 'ADMIN', ttlMs: 500 })
    renderApp('/')
    expect(await screen.findByRole('heading', { name: /Welcome/ })).toBeInTheDocument()
    expect(
      await screen.findByText('Session expired. Please log in again.', {}, { timeout: 3000 }),
    ).toBeInTheDocument()
    expect(sessionStorage.getItem('neuroguard.session')).toBeNull()
  })

  it('goes back to login when the backend rejects the token with 401', async () => {
    server.use(
      http.get(`${API}/api/admin/staff`, () => apiError(401, 'Authentication is required')),
    )
    loginAs({ role: 'ADMIN' })
    renderApp('/admin/staff')

    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(screen.getByRole('status')).toHaveTextContent('Session expired')
    expect(sessionStorage.getItem('neuroguard.session')).toBeNull()
  })

  it('log out clears the session and returns to the login page', async () => {
    loginAs({ role: 'ADMIN' })
    renderApp('/')
    await userEvent.setup().click(await screen.findByRole('button', { name: 'Log out' }))
    expect(await screen.findByRole('heading', { name: 'Log in' })).toBeInTheDocument()
    expect(sessionStorage.getItem('neuroguard.session')).toBeNull()
  })
})

describe('top bar', () => {
  it("shows the logged-in user's name and role, not their email", async () => {
    server.use(
      http.get(`${API}/api/me`, () =>
        HttpResponse.json({
          ...defaultMe,
          name: 'Dr. Nora Neuro',
          email: 'nora@example.com',
          role: 'CLINICAL_STAFF',
          scope: 'NEUROLOGIST',
        }),
      ),
    )
    loginAs({ role: 'CLINICAL_STAFF', scope: 'NEUROLOGIST', email: 'nora@example.com' })
    renderApp('/')

    const banner = await screen.findByRole('banner')
    expect(await within(banner).findByText('Dr. Nora Neuro')).toBeInTheDocument()
    expect(within(banner).getByText('Neurologist')).toBeInTheDocument()
    expect(within(banner).queryByText('nora@example.com')).not.toBeInTheDocument()
  })
})

describe('staff management', () => {
  it('lists staff and asks the backend to sort by the clicked column', async () => {
    const queries: URLSearchParams[] = []
    server.use(
      http.get(`${API}/api/admin/staff`, ({ request }) => {
        queries.push(new URL(request.url).searchParams)
        return HttpResponse.json(
          staffPage([
            staffMember(),
            staffMember({
              id: 'x',
              name: 'Bill Clerk',
              email: 'bill@example.com',
              permissionScope: 'BILLING_CLERK',
            }),
          ]),
        )
      }),
    )
    loginAs({ role: 'ADMIN' })
    renderApp('/admin/staff')

    expect(await screen.findByText('Nora Neuro')).toBeInTheDocument()
    expect(screen.getByText('Bill Clerk')).toBeInTheDocument()
    expect(screen.getByText('Showing 1–2 of 2')).toBeInTheDocument()
    expect(queries[0]?.get('sortBy')).toBe('name')

    await userEvent.setup().click(screen.getByRole('button', { name: /Email/ }))
    await vi.waitFor(() => expect(queries.at(-1)?.get('sortBy')).toBe('email'))
    expect(screen.getByRole('columnheader', { name: /Email/ })).toHaveAttribute(
      'aria-sort',
      'ascending',
    )
  })

  it('shows an empty state and an error state with retry', async () => {
    let fail = true
    server.use(
      http.get(`${API}/api/admin/staff`, () =>
        fail ? apiError(500, 'An unexpected error occurred') : HttpResponse.json(staffPage([])),
      ),
    )
    loginAs({ role: 'ADMIN' })
    renderApp('/admin/staff')

    expect(await screen.findByRole('alert')).toHaveTextContent('An unexpected error occurred')
    fail = false
    await userEvent.setup().click(screen.getByRole('button', { name: 'Try again' }))
    expect(await screen.findByText('No staff accounts yet')).toBeInTheDocument()
  })

  it('creates a staff member, shows a success toast and refreshes the list', async () => {
    const created = vi.fn()
    let members = [
      staffMember({
        name: 'Existing Admin',
        email: 'admin@example.com',
        baseRole: 'ADMIN',
        permissionScope: null,
      }),
    ]
    server.use(
      http.get(`${API}/api/admin/staff`, () => HttpResponse.json(staffPage(members))),
      http.post(`${API}/api/admin/staff`, async ({ request }) => {
        const body = (await request.json()) as Record<string, unknown>
        created(body)
        members = [
          ...members,
          staffMember({ id: 'new', name: String(body.name), email: String(body.email) }),
        ]
        return HttpResponse.json(members.at(-1), { status: 201 })
      }),
    )
    loginAs({ role: 'ADMIN' })
    renderApp('/admin/staff')
    const user = userEvent.setup()

    await screen.findByText('Existing Admin')
    await user.click(screen.getByRole('button', { name: 'Add staff' }))
    const dialog = await screen.findByRole('dialog', { name: 'Add staff' })
    await user.type(within(dialog).getByLabelText('Full name'), 'Bill Clerk')
    await user.type(within(dialog).getByLabelText('Email'), 'bill@example.com')
    await user.type(within(dialog).getByLabelText('Temporary password'), 'longenough1')
    await user.selectOptions(within(dialog).getByLabelText('Permission scope'), 'BILLING_CLERK')
    await user.click(within(dialog).getByRole('button', { name: 'Add staff' }))

    expect(await screen.findByText('Added Bill Clerk')).toBeInTheDocument()
    expect(created).toHaveBeenCalledWith({
      name: 'Bill Clerk',
      email: 'bill@example.com',
      password: 'longenough1',
      baseRole: 'CLINICAL_STAFF',
      permissionScope: 'BILLING_CLERK',
    })
    expect(await screen.findByText('bill@example.com')).toBeInTheDocument()
  })

  it('changes a role and shows the backend error when it is refused', async () => {
    const patch = vi.fn()
    let refuse = true
    server.use(
      http.get(`${API}/api/admin/staff`, () => HttpResponse.json(staffPage())),
      http.patch(`${API}/api/admin/staff/:id/role`, async ({ request }) => {
        patch(await request.json())
        return refuse
          ? apiError(409, 'Cannot demote the last remaining ADMIN')
          : HttpResponse.json(staffMember({ baseRole: 'ADMIN', permissionScope: null }))
      }),
    )
    loginAs({ role: 'ADMIN' })
    renderApp('/admin/staff')
    const user = userEvent.setup()

    await user.click(await screen.findByRole('button', { name: 'Change role for Nora Neuro' }))
    const dialog = await screen.findByRole('dialog', { name: 'Change role' })
    await user.selectOptions(within(dialog).getByLabelText('Role'), 'ADMIN')
    await user.click(within(dialog).getByRole('button', { name: 'Save role' }))
    expect(
      await within(dialog).findByText('Cannot demote the last remaining ADMIN'),
    ).toBeInTheDocument()

    refuse = false
    await user.click(within(dialog).getByRole('button', { name: 'Save role' }))
    expect(await screen.findByText('Updated role for Nora Neuro')).toBeInTheDocument()
    expect(patch).toHaveBeenLastCalledWith({ baseRole: 'ADMIN', permissionScope: null })
  })
})
