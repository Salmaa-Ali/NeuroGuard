import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { AxiosError, type AxiosResponse } from 'axios'
import { describe, expect, it, vi } from 'vitest'
import { renderWithProviders } from '../../test/utils'
import { StaffForm } from './StaffForm'

function setup(onSubmit = vi.fn().mockResolvedValue(undefined)) {
  const user = userEvent.setup()
  renderWithProviders(<StaffForm onSubmit={onSubmit} onCancel={() => {}} />)
  return { user, onSubmit }
}

async function fillBasics(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Full name'), 'Nora Neuro')
  await user.type(screen.getByLabelText('Email'), 'nora@example.com')
  await user.type(screen.getByLabelText('Temporary password'), 'longenough1')
}

function axiosError(status: number, data: unknown) {
  return new AxiosError('failed', 'ERR_BAD_REQUEST', undefined, undefined, {
    status,
    data,
  } as AxiosResponse)
}

describe('StaffForm', () => {
  it('shows the scope dropdown only for CLINICAL_STAFF', async () => {
    const { user } = setup()
    // CLINICAL_STAFF is the default role
    expect(screen.getByLabelText('Permission scope')).toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Role'), 'ADMIN')
    expect(screen.queryByLabelText('Permission scope')).not.toBeInTheDocument()

    await user.selectOptions(screen.getByLabelText('Role'), 'CLINICAL_STAFF')
    expect(screen.getByLabelText('Permission scope')).toBeInTheDocument()
  })

  it('requires a scope for CLINICAL_STAFF', async () => {
    const { user, onSubmit } = setup()
    await fillBasics(user)
    await user.click(screen.getByRole('button', { name: 'Add staff' }))

    expect(await screen.findByText('Select a permission scope')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submits CLINICAL_STAFF with the chosen scope', async () => {
    const { user, onSubmit } = setup()
    await fillBasics(user)
    await user.selectOptions(screen.getByLabelText('Permission scope'), 'NEUROLOGIST')
    await user.click(screen.getByRole('button', { name: 'Add staff' }))

    await vi.waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({
        name: 'Nora Neuro',
        email: 'nora@example.com',
        password: 'longenough1',
        baseRole: 'CLINICAL_STAFF',
        permissionScope: 'NEUROLOGIST',
      }),
    )
  })

  it('submits ADMIN without a scope, even if one was picked before switching', async () => {
    const { user, onSubmit } = setup()
    await fillBasics(user)
    await user.selectOptions(screen.getByLabelText('Permission scope'), 'RADIOLOGIST')
    await user.selectOptions(screen.getByLabelText('Role'), 'ADMIN')
    await user.click(screen.getByRole('button', { name: 'Add staff' }))

    await vi.waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith(
        expect.objectContaining({ baseRole: 'ADMIN', permissionScope: null }),
      ),
    )
  })

  it('checks the password length (8 to 72) and the email format', async () => {
    const { user, onSubmit } = setup()
    await user.type(screen.getByLabelText('Full name'), 'Nora')
    await user.type(screen.getByLabelText('Email'), 'not-an-email')
    await user.type(screen.getByLabelText('Temporary password'), 'short')
    await user.selectOptions(screen.getByLabelText('Role'), 'ADMIN')
    await user.click(screen.getByRole('button', { name: 'Add staff' }))

    expect(await screen.findByText('Password must be at least 8 characters')).toBeInTheDocument()
    expect(screen.getByText('Enter a valid email')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('puts a duplicate-email 409 on the email field', async () => {
    const onSubmit = vi.fn().mockRejectedValue(
      axiosError(409, {
        message: 'A staff account with this email already exists',
        fieldErrors: [],
      }),
    )
    const { user } = setup(onSubmit)
    await fillBasics(user)
    await user.selectOptions(screen.getByLabelText('Permission scope'), 'NEUROLOGIST')
    await user.click(screen.getByRole('button', { name: 'Add staff' }))

    await vi.waitFor(() =>
      expect(screen.getByLabelText('Email')).toHaveAccessibleDescription(
        'A staff account with this email already exists',
      ),
    )
  })

  it('maps backend field errors onto the form fields', async () => {
    const onSubmit = vi.fn().mockRejectedValue(
      axiosError(400, {
        message: 'Validation failed',
        fieldErrors: [{ field: 'name', message: 'must not be blank' }],
      }),
    )
    const { user } = setup(onSubmit)
    await fillBasics(user)
    await user.selectOptions(screen.getByLabelText('Permission scope'), 'NEUROLOGIST')
    await user.click(screen.getByRole('button', { name: 'Add staff' }))

    await vi.waitFor(() =>
      expect(screen.getByLabelText('Full name')).toHaveAccessibleDescription('must not be blank'),
    )
  })
})
