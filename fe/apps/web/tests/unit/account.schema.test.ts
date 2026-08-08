import { describe, expect, it } from 'vitest'

import {
  accountsResponseSchema,
  registerAccountRequestSchema,
} from '@/schemas/account.schema'

describe('account schemas', () => {
  it('ApiResponse로 감싼 계좌 배열을 파싱한다', () => {
    const response = accountsResponseSchema.parse({
      success: true,
      data: [
        {
          accountId: 7,
          bankCode: '004',
          bankName: 'KB국민은행',
          accountNo: '12345612123456',
        },
      ],
      message: null,
    })

    expect(response.data[0]?.bankCode).toBe('004')
  })

  it('금융결제원 코드와 계좌 등록 요청 형식을 검증한다', () => {
    expect(
      registerAccountRequestSchema.safeParse({
        bankCode: '004',
        accountNo: '12345612123456',
        accountPassword: '1004',
      }).success,
    ).toBe(true)
    expect(
      registerAccountRequestSchema.safeParse({
        bankCode: 'KB',
        accountNo: '12345612123456',
        accountPassword: '1004',
      }).success,
    ).toBe(false)
    expect(
      registerAccountRequestSchema.safeParse({
        bankCode: '004',
        accountNo: '12345678901234567',
        accountPassword: '1004',
      }).success,
    ).toBe(false)
  })
})
