import { describe, expect, it } from 'vitest'

import { getUserIdFromAccessToken } from '@/api/token-storage'

function createAccessToken(subject: string) {
  return `header.${btoa(JSON.stringify({ sub: subject }))}.signature`
}

describe('getUserIdFromAccessToken', () => {
  it('returns a positive numeric JWT subject', () => {
    expect(getUserIdFromAccessToken(createAccessToken('42'))).toBe(42)
  })

  it('returns null for a malformed or invalid JWT subject', () => {
    expect(getUserIdFromAccessToken('not-a-jwt')).toBeNull()
    expect(getUserIdFromAccessToken(createAccessToken('ward'))).toBeNull()
  })
})
