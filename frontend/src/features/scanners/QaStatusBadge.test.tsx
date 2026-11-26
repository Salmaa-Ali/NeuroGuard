import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { QaStatus } from '@/api/types'
import { QaStatusBadge, RiskScore } from './QaStatusBadge'
import { QA_STATUS_META, formatLimits, isWithin } from './qa'

describe('QA status badge mapping', () => {
  const cases: [QaStatus, string, boolean][] = [
    ['OK', 'success', false],
    ['DUE_SOON', 'warning', false],
    ['OVERDUE', 'danger', true],
    ['FAILED', 'danger', true],
    ['NEVER_CALIBRATED', 'neutral', false],
  ]

  it.each(cases)('%s is %s, flagged=%s', (status, tone, flagged) => {
    render(<QaStatusBadge status={status} />)
    const badge = screen.getByText(QA_STATUS_META[status].label, { exact: false })
    expect(badge.closest('[data-tone]')).toHaveAttribute('data-tone', tone)
    if (flagged) expect(badge.closest('[data-tone]')).toHaveTextContent('Flagged for review')
    else expect(badge.closest('[data-tone]')).not.toHaveTextContent('Flagged for review')
  })

  it('never calls a scanner unsafe', () => {
    for (const meta of Object.values(QA_STATUS_META))
      expect(meta.label.toLowerCase()).not.toContain('unsafe')
  })

  it('labels the risk score as indicative', () => {
    render(<RiskScore value={42.25} withLabel />)
    expect(screen.getByText(/Risk score \(indicative\)/)).toHaveTextContent('42.3')
  })
})

describe('tolerance helpers (same rule as the backend)', () => {
  it('uses inclusive bounds and treats a missing bound as no limit', () => {
    expect(isWithin(188, 188, 192)).toBe(true)
    expect(isWithin(192, 188, 192)).toBe(true)
    expect(isWithin(192.01, 188, 192)).toBe(false)
    expect(isWithin(87.4, 87.5, null)).toBe(false)
    expect(isWithin(-1000, null, 2.5)).toBe(true)
    expect(isWithin(5, null, null)).toBe(true)
  })

  it('formats limits', () => {
    expect(formatLimits(188, 192, 'mm')).toBe('188 – 192 mm')
    expect(formatLimits(null, 2.5, '%')).toBe('≤ 2.5 %')
    expect(formatLimits(87.5, null, '%')).toBe('≥ 87.5 %')
    expect(formatLimits(null, null)).toBe('No limits')
  })
})
