import { describe, expect, it } from 'vitest'

import { parseWardIdQuery } from '@/pages/guard/-composables/useGuardWardQuery'

describe('parseWardIdQuery', () => {
  it.each([
    ['12', 12],
    [['13'], 13],
    ['0', null],
    ['-1', null],
    ['1.5', null],
    ['ward-12', null],
    [undefined, null],
  ])('%j를 숫자 wardId로 정규화한다', (value, expected) => {
    expect(parseWardIdQuery(value)).toBe(expected)
  })
})
