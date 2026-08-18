import { describe, expect, it } from 'vitest'

import {
  digitsOnly,
  formatBirthDate,
  formatPhoneNumber,
} from '@/pages/auth/sign-up/details/-utils/sign-up-details-format'

describe('회원가입 상세 입력 포맷', () => {
  it('휴대폰 번호를 숫자 11자리 기준으로 포맷한다', () => {
    expect(formatPhoneNumber('01012345678')).toBe('010-1234-5678')
    expect(formatPhoneNumber('010-12a34-567890')).toBe('010-1234-5678')
  })

  it('생년월일을 숫자 8자리 기준으로 포맷한다', () => {
    expect(formatBirthDate('19900102')).toBe('1990.01.02')
    expect(formatBirthDate('1990년 01월 0299')).toBe('1990.01.02')
  })

  it('인증번호처럼 포맷이 필요 없는 값은 숫자와 길이만 제한한다', () => {
    expect(digitsOnly('12a34-567', 6)).toBe('123456')
  })
})
