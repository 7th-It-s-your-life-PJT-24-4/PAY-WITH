import { describe, expect, it } from 'vitest'

import { signUpDetailsSchema } from '@/schemas/sign-up.schema'

describe('signUpDetailsSchema', () => {
  const validDetails = {
    fullName: '홍길동',
    phoneNumber: '010-1234-5678',
    birthDate: '1990.01.02',
    gender: '남',
    loginPassword: 'password123',
    paymentPassword: '123456',
    serviceTerms: true,
    privacyTerms: true,
    identifierTerms: true,
  }

  it('점으로 구분된 생년월일을 API 전송 형식으로 정규화한다', () => {
    const result = signUpDetailsSchema.parse(validDetails)

    expect(result).toMatchObject({
      phoneNumber: '01012345678',
      birthDate: '19900102',
      gender: '남',
    })
  })

  it.each([
    ['한 글자 성함', { fullName: '홍' }],
    ['숫자가 포함된 성함', { fullName: '홍길동1' }],
    ['공백이 포함된 성함', { fullName: '홍 길동' }],
    ['앞뒤 공백이 포함된 성함', { fullName: ' 홍길동 ' }],
    ['형식에 맞지 않는 휴대폰 번호', { phoneNumber: '010-12-345' }],
    ['존재하지 않는 날짜', { birthDate: '1990.02.30' }],
    ['미래 날짜', { birthDate: '2999.01.01' }],
    ['성별 미선택', { gender: undefined }],
    ['영문이 없는 로그인 비밀번호', { loginPassword: '12345678' }],
    ['공백이 포함된 로그인 비밀번호', { loginPassword: 'password 123' }],
    ['숫자가 아닌 결제 비밀번호', { paymentPassword: '12345a' }],
    ['미동의 필수 약관', { privacyTerms: false }],
  ])('%s을 거부한다', (_, invalidValue) => {
    expect(
      signUpDetailsSchema.safeParse({ ...validDetails, ...invalidValue })
        .success,
    ).toBe(false)
  })
})
