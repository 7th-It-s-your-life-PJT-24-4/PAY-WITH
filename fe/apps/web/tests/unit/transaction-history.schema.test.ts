import { describe, expect, it } from 'vitest'

import { wardTransactionHistoryResponseSchema } from '@/schemas/transaction-history.schema'

describe('transaction history schemas', () => {
  it('위험 평가가 없는 충전 내역을 파싱한다', () => {
    const response = wardTransactionHistoryResponseSchema.parse({
      success: true,
      data: {
        transactions: [
          {
            transactionId: 1,
            type: 'CHARGE',
            direction: 'IN',
            title: 'KB국민은행',
            amount: 50_000,
            status: 'COMPLETED',
            riskLevel: null,
            occurredAt: '2026-08-05T09:30:00',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
        hasNext: false,
      },
      message: null,
    })

    expect(response.data.transactions[0]?.riskLevel).toBeNull()
  })
})
