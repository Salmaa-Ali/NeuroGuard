import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import type { QaTest } from '@/api/types'
import { renderWithProviders } from '@/test/utils'
import { CalibrationForm } from './CalibrationForm'

const NOW = new Date('2026-06-15T10:00:00')

const test = (over: Partial<QaTest>): QaTest => ({
  id: 'id',
  code: 'CODE',
  name: 'Name',
  unit: 'mm',
  lowerLimit: null,
  upperLimit: null,
  reference: null,
  placeholder: false,
  active: true,
  ...over,
})

const TESTS = [
  test({
    id: 'geom',
    code: 'ACR_GEOM',
    name: 'Geometric accuracy',
    lowerLimit: 188,
    upperLimit: 192,
    placeholder: true,
  }),
  test({
    id: 'ghost',
    code: 'ACR_GHOST',
    name: 'Percent signal ghosting',
    unit: '%',
    upperLimit: 2.5,
  }),
]

function setup(onSubmit = vi.fn().mockResolvedValue(undefined)) {
  const user = userEvent.setup()
  renderWithProviders(
    <CalibrationForm tests={TESTS} onSubmit={onSubmit} onCancel={() => {}} now={() => NOW} />,
  )
  return { user, onSubmit }
}

describe('CalibrationForm', () => {
  it('shows each test with its limits and the placeholder label', () => {
    setup()
    expect(screen.getByText('188 – 192 mm')).toBeInTheDocument()
    expect(screen.getByText('≤ 2.5 %')).toBeInTheDocument()
    expect(screen.getAllByText('Placeholder: confirm with medical physicist')).toHaveLength(1)
  })

  it('previews within / out of tolerance while typing', async () => {
    const { user } = setup()
    await user.type(screen.getByLabelText('Value for Geometric accuracy'), '190')
    expect(screen.getByTestId('hint-ACR_GEOM')).toHaveTextContent('Within tolerance')

    await user.clear(screen.getByLabelText('Value for Geometric accuracy'))
    await user.type(screen.getByLabelText('Value for Geometric accuracy'), '192.5')
    expect(screen.getByTestId('hint-ACR_GEOM')).toHaveTextContent('Out of tolerance')

    await user.type(screen.getByLabelText('Value for Percent signal ghosting'), '2.5')
    expect(screen.getByTestId('hint-ACR_GHOST')).toHaveTextContent('Within tolerance')
  })

  it('requires at least one value', async () => {
    const { user, onSubmit } = setup()
    await user.click(screen.getByRole('button', { name: 'Save session' }))
    expect(await screen.findByText('Enter at least one measured value')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('rejects a date in the future and a non-numeric value', async () => {
    const { user, onSubmit } = setup()
    const date = screen.getByLabelText('Performed at')
    await user.clear(date)
    await user.type(date, '2026-06-16T09:00')
    await user.type(screen.getByLabelText('Value for Geometric accuracy'), 'abc')
    await user.click(screen.getByRole('button', { name: 'Save session' }))
    expect(await screen.findByText('Date must not be in the future')).toBeInTheDocument()
    expect(screen.getByText('Enter a number')).toBeInTheDocument()
    expect(onSubmit).not.toHaveBeenCalled()
  })

  it('submits only the rows with a value, as numbers, with an ISO date', async () => {
    const { user, onSubmit } = setup()
    await user.type(screen.getByLabelText('Value for Geometric accuracy'), '190.5')
    await user.type(screen.getByLabelText('Certificate ref'), 'CERT-1')
    await user.click(screen.getByRole('button', { name: 'Save session' }))
    await vi.waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith({
        performedAt: new Date('2026-06-15T10:00').toISOString(),
        notes: undefined,
        certificateRef: 'CERT-1',
        measurements: [{ qaTestId: 'geom', measuredValue: 190.5 }],
      }),
    )
  })
})
