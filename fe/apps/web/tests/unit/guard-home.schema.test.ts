import { describe, expect, it } from 'vitest'

import { guardHomeResponseSchema } from '@/schemas/guard-home.schema'

describe('guard home schema', () => {
  it('선택한 시니어의 승인 대기 거래 건수를 파싱한다', () => {
    const result = guardHomeResponseSchema.parse({
      success: true,
      data: {
        wards: [
          {
            wardId: 12,
            name: '김시니어',
            avatarId: 1,
            hasPending: true,
          },
        ],
        selectedWard: {
          wardId: 12,
          name: '김시니어',
          balance: 120_000,
          pendingApprovals: [
            {
              transactionId: 41,
              amount: 35_000,
              holderName: '박수취',
              accountNo: '110234567890',
              riskScore: 87,
              riskReason: '메모에 위험 키워드 포함',
              createdAt: '2026-08-05T09:10:00',
            },
          ],
          pendingApprovalCount: 3,
          recentTransactions: [],
        },
      },
      message: null,
    })

    expect(result.data.selectedWard?.pendingApprovalCount).toBe(3)
    expect(result.data.selectedWard?.pendingApprovals).toHaveLength(1)
  })
})
