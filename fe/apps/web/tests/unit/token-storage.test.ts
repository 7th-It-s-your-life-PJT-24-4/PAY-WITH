import { describe, expect, it } from 'vitest'

import {
  getAccessTokenExpiresAt,
  getUserIdFromAccessToken,
  isAccessTokenExpiring,
} from '@/api/token-storage'

function createAccessToken(subject: string, expiresAt?: number) {
  return `header.${btoa(JSON.stringify({ sub: subject, exp: expiresAt }))}.signature`
}

describe('getUserIdFromAccessToken', () => {
  it('returns a positive numeric JWT subject', () => {
    expect(getUserIdFromAccessToken(createAccessToken('42'))).toBe(42)
  })

  it('returns null for a malformed or invalid JWT subject', () => {
    expect(getUserIdFromAccessToken('not-a-jwt')).toBeNull()
    expect(getUserIdFromAccessToken(createAccessToken('ward'))).toBeNull()
  })

  it('reads the JWT expiration and detects an expiring token', () => {
    const expiresAt = Math.floor((Date.now() + 30_000) / 1_000)
    const token = createAccessToken('42', expiresAt)

    expect(getAccessTokenExpiresAt(token)).toBe(expiresAt * 1_000)
    expect(isAccessTokenExpiring(token)).toBe(true)
    expect(isAccessTokenExpiring(token, 10_000)).toBe(false)
  })
})
