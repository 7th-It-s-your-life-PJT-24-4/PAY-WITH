import { describe, expect, it } from 'vitest'

import {
  guardTransactionDetailResponseSchema,
  guardTransactionHistoryResponseSchema,
} from '@/schemas/transaction.schema'

describe('guard transaction response schema', () => {
  it('보호자용 거래 목록과 null 위험도를 파싱한다', () => {
    const result = guardTransactionHistoryResponseSchema.parse({
      success: true,
      data: {
        transactions: [
          {
            transactionId: 40,
            type: 'CHARGE',
            status: 'COMPLETED',
            counterpartyName: 'KB국민은행',
            amount: 50000,
            riskLevel: null,
            riskReason: null,
            createdAt: '2026-08-05T09:20:01',
          },
        ],
        page: 0,
        size: 100,
        totalElements: 1,
        totalPages: 1,
        hasNext: false,
      },
      message: null,
    })

    expect(result.data.transactions[0]).toMatchObject({
      transactionId: 40,
      riskLevel: null,
    })
  })

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
        riskAnalysis: null,
        occurredAt: '2026-08-05T10:00:00',
      },
      message: null,
    })

    expect(result.data.riskAnalysis).toBeNull()
  })

  it('SAFE 거래의 위험 분석 점수를 파싱한다', () => {
    const result = guardTransactionDetailResponseSchema.parse({
      success: true,
      data: {
        transactionId: 43,
        type: 'PAYMENT',
        direction: 'OUT',
        status: 'COMPLETED',
        riskLevel: 'SAFE',
        counterpartyName: '동네 약국',
        bankName: null,
        accountNo: null,
        amount: 12000,
        memo: null,
        balanceAfter: 158000,
        riskAnalysis: {
          riskScore: 8,
          summary: null,
          reasons: [],
        },
        occurredAt: '2026-08-07T10:30:00',
      },
      message: null,
    })

    expect(result.data.riskLevel).toBe('SAFE')
    expect(result.data.riskAnalysis?.riskScore).toBe(8)
  })
})
