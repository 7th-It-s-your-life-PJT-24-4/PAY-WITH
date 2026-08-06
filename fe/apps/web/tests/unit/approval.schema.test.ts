import { describe, expect, it } from 'vitest'

import {
  approvalDecisionResponseSchema,
  approvalRequestDetailResponseSchema,
  approvalRequestListResponseSchema,
} from '@/schemas/approval.schema'

const detail = {
  approvalId: 3,
  wardId: 12,
  wardName: '김시니어',
  amount: 35000,
  memo: '생활비',
  holderName: '박수취',
  bankName: '신한은행',
  accountNo: '110234567890',
  riskLevel: 'DANGER',
  totalScore: 87,
  requestedAt: '2026-08-05T09:10:00',
  expiredAt: '2026-08-05T09:40:00',
  ruleHits: [
    { ruleCode: 'SUSPICIOUS_MEMO', description: '메모에 위험 키워드 포함' },
  ],
}

describe('approval response schemas', () => {
  it('승인 이력에서 거래 상세 이동에 필요한 거래 ID와 상태를 파싱한다', () => {
    const result = approvalRequestListResponseSchema.parse({
      success: true,
      data: [
        {
          approvalId: 3,
          transactionId: 41,
          wardId: 12,
          wardName: '김시니어',
          amount: 35000,
          holderName: '박수취',
          bankName: '신한은행',
          riskLevel: 'DANGER',
          requestedAt: '2026-08-05T09:10:00',
          expiredAt: '2026-08-05T12:10:00',
          status: 'APPROVED',
          respondedAt: '2026-08-05T09:20:00',
        },
      ],
      message: null,
    })

    expect(result.data[0]).toMatchObject({
      transactionId: 41,
      status: 'APPROVED',
    })
  })

  it('보호자가 확인할 위험 점수와 룰 근거를 파싱한다', () => {
    const result = approvalRequestDetailResponseSchema.parse({
      success: true,
      data: detail,
      message: null,
    })

    expect(result.data.totalScore).toBe(87)
    expect(result.data.ruleHits[0]?.description).toBe('메모에 위험 키워드 포함')
  })

  it('승인은 성공했지만 송금은 실패한 응답을 구분한다', () => {
    const result = approvalDecisionResponseSchema.parse({
      success: true,
      data: {
        approvalId: 3,
        transactionId: 41,
        status: 'APPROVED',
        respondedAt: '2026-08-05T09:20:00',
        transfer: {
          status: 'FAILED',
          failureReason: '송금 가능한 잔액이 부족합니다.',
          completedAt: null,
          balanceAfter: null,
        },
      },
      message: null,
    })

    expect(result.data.status).toBe('APPROVED')
    expect(result.data.transfer?.status).toBe('FAILED')
  })
})
