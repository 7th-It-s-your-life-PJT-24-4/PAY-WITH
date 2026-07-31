import { describe, expect, it } from 'vitest'

import {
  chargeAccountsResponseSchema,
  chargeResultResponseSchema,
  registerChargeAccountRequestSchema,
} from '@/schemas/charge.schema'

describe('charge schemas', () => {
  it('ApiResponse로 감싼 계좌 배열을 파싱한다', () => {
    const response = chargeAccountsResponseSchema.parse({
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
      registerChargeAccountRequestSchema.safeParse({
        bankCode: '004',
        accountNo: '12345612123456',
        accountPassword: '1004',
      }).success,
    ).toBe(true)
    expect(
      registerChargeAccountRequestSchema.safeParse({
        bankCode: 'KB',
        accountNo: '12345612123456',
        accountPassword: '1004',
      }).success,
    ).toBe(false)
  })

  it('status 필드 없이 충전 성공 응답을 파싱한다', () => {
    const response = chargeResultResponseSchema.parse({
      success: true,
      data: {
        transactionId: 43,
        chargeAmount: 50_000,
        balanceAfter: 150_000,
        bankName: 'KB국민은행',
        accountNo: '12345612123456',
        createdAt: '2026-07-23T17:30:00',
      },
      message: null,
    })

    expect(response.data.chargeAmount).toBe(50_000)
    expect('status' in response.data).toBe(false)
  })
})
