import { describe, expect, it } from 'vitest'

import { guardTransactionDetailResponseSchema } from '@/schemas/transaction.schema'

describe('guard transaction detail response schema', () => {
  it('동료 백엔드의 위험 분석 응답을 파싱한다', () => {
    const result = guardTransactionDetailResponseSchema.parse({
      success: true,
      data: {
        transactionId: 41,
        type: 'TRANSFER',
        direction: 'OUT',
        status: 'COMPLETED',
        riskLevel: 'DANGER',
        counterpartyName: '박수취',
        bankName: '신한은행',
        accountNo: '110234567890',
        amount: 35000,
        memo: '생활비',
        balanceAfter: 120000,
        riskScore: null,
        riskAnalysis: {
          riskScore: 87,
          summary: '평소와 다른 고액 송금이에요.',
          reasons: ['HIGH_AMOUNT_L2', 'NEW_RECIPIENT'],
        },
        occurredAt: '2026-08-05T09:20:01',
      },
      message: null,
    })

    expect(result.data.riskAnalysis).toMatchObject({
      riskScore: 87,
      reasons: ['HIGH_AMOUNT_L2', 'NEW_RECIPIENT'],
    })
  })

  it('평가 대상이 아닌 거래의 null 위험 분석을 허용한다', () => {
    const result = guardTransactionDetailResponseSchema.parse({
      success: true,
      data: {
        transactionId: 42,
        type: 'CHARGE',
        direction: 'IN',
        status: 'COMPLETED',
        riskLevel: null,
        counterpartyName: null,
        bankName: null,
        accountNo: null,
        amount: 50000,
        memo: null,
        balanceAfter: 170000,
        riskScore: null,
        riskAnalysis: null,
        occurredAt: '2026-08-05T10:00:00',
      },
      message: null,
    })

    expect(result.data.riskAnalysis).toBeNull()
  })
})
