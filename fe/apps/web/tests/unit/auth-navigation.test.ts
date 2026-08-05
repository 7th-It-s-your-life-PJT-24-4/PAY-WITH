import { describe, expect, it } from 'vitest'

import { getRoleHomePath, getSafePostLoginPath } from '@/router/auth-navigation'

describe('getRoleHomePath', () => {
  it('보호자는 guard 홈으로 이동한다', () => {
    expect(getRoleHomePath('GUARD')).toBe('/guard')
  })

  it('시니어는 ward 홈으로 이동한다', () => {
    expect(getRoleHomePath('WARD')).toBe('/ward')
  })
})

describe('getSafePostLoginPath', () => {
  it('allows safe authenticated pages', () => {
    expect(getSafePostLoginPath('/ward?tab=recent', 'WARD')).toBe(
      '/ward?tab=recent',
    )
    expect(getSafePostLoginPath('/ward/history', 'WARD')).toBe('/ward/history')
    expect(getSafePostLoginPath('/guard/history', 'GUARD')).toBe(
      '/guard/history',
    )
  })

  it('rejects auth, external, other-role, and transaction flow paths', () => {
    expect(getSafePostLoginPath('//example.com', 'WARD')).toBeNull()
    expect(getSafePostLoginPath('/auth/sign-in', 'WARD')).toBeNull()
    expect(getSafePostLoginPath('/guard', 'WARD')).toBeNull()
    expect(getSafePostLoginPath('/ward/payment/qr/1', 'WARD')).toBeNull()
    expect(getSafePostLoginPath('/ward/transfer/processing', 'WARD')).toBeNull()
    expect(getSafePostLoginPath('/guard/charge', 'GUARD')).toBeNull()
  })
})
