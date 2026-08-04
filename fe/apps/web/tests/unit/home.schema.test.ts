import { describe, expect, it } from 'vitest'

import {
  wardApprovalDetailResponseSchema,
  wardHomeResponseSchema,
} from '@/schemas/home.schema'

const pendingApproval = {
  approvalId: 7,
  transactionId: 74,
  type: 'TRANSFER_OUT',
  amount: 50_000,
  holderName: '김민수',
  bankName: '국민은행',
  accountNo: '43210201234567',
  riskLevel: 'CAUTION',
  requestedAt: '2026-08-04T10:00:00',
  expiredAt: '2026-08-04T10:10:00',
} as const

describe('ward home schema', () => {
  it('홈의 지갑과 송금 승인 대기 목록을 파싱한다', () => {
    const result = wardHomeResponseSchema.parse({
      success: true,
      data: {
        userName: '김시니어',
        wallet: {
          walletId: 9207,
          balance: 500_000,
          updatedAt: '2026-08-04T10:00:00',
        },
        pendingApprovalCount: 1,
        pendingApprovals: [pendingApproval],
      },
      message: null,
    })

    expect(result.data.wallet.balance).toBe(500_000)
    expect(result.data.pendingApprovals[0]?.type).toBe('TRANSFER_OUT')
  })

  it('결제 승인 대기 항목은 거부한다', () => {
    const result = wardHomeResponseSchema.safeParse({
      success: true,
      data: {
        userName: '김시니어',
        wallet: {
          walletId: 9207,
          balance: 500_000,
          updatedAt: '2026-08-04T10:00:00',
        },
        pendingApprovalCount: 1,
        pendingApprovals: [{ ...pendingApproval, type: 'PAYMENT' }],
      },
      message: null,
    })

    expect(result.success).toBe(false)
  })

  it('승인 대기 상세의 메모를 파싱한다', () => {
    const result = wardApprovalDetailResponseSchema.parse({
      success: true,
      data: { ...pendingApproval, memo: '생활비' },
      message: null,
    })

    expect(result.data.memo).toBe('생활비')
  })
})
