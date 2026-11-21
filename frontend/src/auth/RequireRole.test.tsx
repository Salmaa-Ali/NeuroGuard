import { screen } from '@testing-library/react'
import { Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import type { PermissionScope, Role } from '../api/types'
import { loginAs, renderWithProviders } from '../test/utils'
import { RequireRole } from './RequireRole'

function renderGuard(props: { roles?: Role[]; scopes?: PermissionScope[] }) {
  return renderWithProviders(
    <Routes>
      <Route path="/login" element={<p>LOGIN PAGE</p>} />
      <Route
        path="/"
        element={
          <RequireRole {...props}>
            <p>SECRET AREA</p>
          </RequireRole>
        }
      />
    </Routes>,
  )
}

const DENIED = "You don't have access to this page"

describe('RequireRole', () => {
  it('renders the page for an allowed role', () => {
    loginAs({ role: 'ADMIN' })
    renderGuard({ roles: ['ADMIN'] })
    expect(screen.getByText('SECRET AREA')).toBeInTheDocument()
  })

  it('shows the no-access screen (and keeps the user logged in) for a role that is not allowed', () => {
    loginAs({ role: 'PATIENT' })
    renderGuard({ roles: ['ADMIN'] })
    expect(screen.getByRole('heading', { name: DENIED })).toBeInTheDocument()
    expect(screen.queryByText('SECRET AREA')).not.toBeInTheDocument()
    expect(sessionStorage.getItem('neuroguard.session')).not.toBeNull()
  })

  it('allows by permission scope', () => {
    loginAs({ role: 'CLINICAL_STAFF', scope: 'NEUROLOGIST' })
    renderGuard({ scopes: ['NEUROLOGIST', 'RADIOLOGIST'] })
    expect(screen.getByText('SECRET AREA')).toBeInTheDocument()
  })

  it('blocks a scope that is not listed', () => {
    loginAs({ role: 'CLINICAL_STAFF', scope: 'BILLING_CLERK' })
    renderGuard({ scopes: ['NEUROLOGIST'] })
    expect(screen.getByRole('heading', { name: DENIED })).toBeInTheDocument()
  })

  it('allows either a listed role OR a listed scope', () => {
    for (const user of [
      { role: 'ADMIN' as const },
      { role: 'CLINICAL_STAFF' as const, scope: 'RADIOLOGIST' as const },
    ]) {
      loginAs(user)
      const { unmount } = renderGuard({ roles: ['ADMIN'], scopes: ['RADIOLOGIST'] })
      expect(screen.getByText('SECRET AREA')).toBeInTheDocument()
      unmount()
    }
    loginAs({ role: 'PATIENT' })
    renderGuard({ roles: ['ADMIN'], scopes: ['RADIOLOGIST'] })
    expect(screen.getByRole('heading', { name: DENIED })).toBeInTheDocument()
  })

  it('allows any logged-in user when no rule is given', () => {
    loginAs({ role: 'PATIENT' })
    renderGuard({})
    expect(screen.getByText('SECRET AREA')).toBeInTheDocument()
  })

  it('sends anyone who is not logged in to /login', () => {
    renderGuard({ roles: ['ADMIN'] })
    expect(screen.getByText('LOGIN PAGE')).toBeInTheDocument()
  })
})
