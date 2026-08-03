import { describe, expect, it } from 'vitest'

import { chargeResultResponseSchema } from '@/schemas/charge.schema'

describe('charge schemas', () => {
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
