import { describe, expect, it } from 'vitest'

import { formatCompactAccount } from '@/pages/ward/charge/-utils/account-format'

describe('formatCompactAccount', () => {
  it('은행명과 계좌번호 끝 4자리를 은행명(끝4자리) 형태로 포맷한다', () => {
    expect(formatCompactAccount('우리은행', '1002-123-456789')).toBe(
      '우리은행(6789)',
    )
    expect(formatCompactAccount('우리은행', '5321')).toBe('우리은행(5321)')
    expect(formatCompactAccount('KB국민은행', '43210201234567')).toBe(
      'KB국민은행(4567)',
    )
  })

  it('계좌번호에 하이픈이나 공백이 있어도 숫자 기준 끝 4자리를 추출한다', () => {
    expect(formatCompactAccount('신한은행', '110-123-456789')).toBe(
      '신한은행(6789)',
    )
  })

  it('계좌번호가 4자리 미만이면 계좌번호 전체를 괄호에 넣는다', () => {
    expect(formatCompactAccount('토스뱅크', '123')).toBe('토스뱅크(123)')
  })

  it('은행명이나 계좌번호가 비어있을 때 안전하게 fallback 처리한다', () => {
    expect(formatCompactAccount('우리은행', '')).toBe('우리은행')
    expect(formatCompactAccount('', '123456')).toBe('3456')
    expect(formatCompactAccount(null, null)).toBe('')
  })
})
