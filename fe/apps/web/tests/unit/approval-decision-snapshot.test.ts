import { beforeEach, describe, expect, it } from 'vitest'

import {
  getApprovalDecisionSnapshot,
  getApprovalDecisionSnapshots,
  saveApprovalDecisionSnapshot,
} from '@/pages/guard/approval-requests/-utils/approval-decision-snapshot'
import type {
  ApprovalDecision,
  ApprovalRequestDetail,
} from '@/schemas/approval.schema'

function createDetail(approvalId: number): ApprovalRequestDetail {
  return {
    approvalId,
    wardId: 12,
    wardName: '김시니어',
    amount: 35_000,
    memo: '생활비',
    holderName: '박수취',
    bankName: '신한은행',
    accountNo: '110234567890',
    riskLevel: 'DANGER',
    totalScore: 87,
    requestedAt: '2026-08-05T09:10:00',
    expiredAt: '2026-08-05T09:40:00',
    ruleHits: [],
  }
}

function createDecision(
  approvalId: number,
  status: ApprovalDecision['status'],
  respondedAt: string,
): ApprovalDecision {
  return {
    approvalId,
    transactionId: approvalId + 100,
    status,
    respondedAt,
    transfer: null,
  }
}

describe('guard approval decision snapshots', () => {
  beforeEach(() => sessionStorage.clear())

  it('처리된 이상 거래를 승인 ID별로 복원한다', () => {
    const detail = createDetail(3)
    const decision = createDecision(3, 'APPROVED', '2026-08-05T09:20:00')

    saveApprovalDecisionSnapshot(detail, decision)

    expect(getApprovalDecisionSnapshot(3)).toEqual({ detail, decision })
  })

  it('승인·거절 목록을 최근 처리 순으로 반환한다', () => {
    saveApprovalDecisionSnapshot(
      createDetail(3),
      createDecision(3, 'APPROVED', '2026-08-05T09:20:00'),
    )
    saveApprovalDecisionSnapshot(
      createDetail(4),
      createDecision(4, 'REJECTED', '2026-08-05T09:25:00'),
    )

    expect(
      getApprovalDecisionSnapshots().map(({ decision }) => decision.status),
    ).toEqual(['REJECTED', 'APPROVED'])
  })
})
