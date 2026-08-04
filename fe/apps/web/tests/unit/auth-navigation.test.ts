import { describe, expect, it } from 'vitest'

import { getRoleHomePath } from '@/router/auth-navigation'

describe('getRoleHomePath', () => {
  it('보호자는 guard 홈으로 이동한다', () => {
    expect(getRoleHomePath('GUARD')).toBe('/guard')
  })

  it('시니어는 ward 홈으로 이동한다', () => {
    expect(getRoleHomePath('WARD')).toBe('/ward')
  })
})
