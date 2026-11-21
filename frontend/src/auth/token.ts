/** Display-only JWT helpers. The backend verifies tokens; the client never needs to. */

export interface JwtPayload {
  sub?: string
  exp?: number
  iat?: number
  iss?: string
  user_type?: string
  role?: string
  perm_scope?: string
}

export function decodeJwtPayload(token: string): JwtPayload | null {
  const part = token.split('.')[1]
  if (!part) return null
  try {
    const base64 = part.replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), '=')
    const bytes = Uint8Array.from(atob(padded), (c) => c.charCodeAt(0))
    return JSON.parse(new TextDecoder().decode(bytes)) as JwtPayload
  } catch {
    return null
  }
}

/** The user's id is the "sub" claim. */
export function getUserId(token: string): string | null {
  return decodeJwtPayload(token)?.sub ?? null
}

/** Expiry as epoch milliseconds, from the "exp" claim. */
export function getExpiryMs(token: string): number | null {
  const exp = decodeJwtPayload(token)?.exp
  return typeof exp === 'number' ? exp * 1000 : null
}
