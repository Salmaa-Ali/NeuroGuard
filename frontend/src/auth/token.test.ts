import { describe, expect, it } from 'vitest'
import { makeJwt } from '../test/utils'
import { decodeJwtPayload, getExpiryMs, getUserId } from './token'

describe('token helpers', () => {
  it('reads the user id (sub) and expiry from the payload', () => {
    const token = makeJwt({ sub: 'abc-123', exp: 1_900_000_000, role: 'ADMIN' })
    expect(getUserId(token)).toBe('abc-123')
    expect(getExpiryMs(token)).toBe(1_900_000_000_000)
    expect(decodeJwtPayload(token)?.role).toBe('ADMIN')
  })

  it('handles non-ASCII text in claims', () => {
    expect(decodeJwtPayload(makeJwt({ sub: 'x', name: 'Zoë' }))).toMatchObject({ sub: 'x' })
  })

  it('returns null for garbage instead of throwing', () => {
    expect(decodeJwtPayload('not-a-token')).toBeNull()
    expect(decodeJwtPayload('a.%%%.c')).toBeNull()
    expect(getUserId('')).toBeNull()
    expect(getExpiryMs('a.e30.c')).toBeNull()
  })
})
