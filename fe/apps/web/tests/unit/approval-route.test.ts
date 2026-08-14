import { describe, expect, it } from 'vitest'

import {
  parseApprovalDecision,
  parseApprovalFilter,
  parseApprovalTransferStatus,
  parseOptionalQueryText,
} from '@/pages/guard/approval-requests/-utils/approval-route'

describe('승인 화면 query 파서', () => {
  it('지원하는 목록 필터만 허용하고 나머지는 대기로 복구한다', () => {
    expect(parseApprovalFilter('approved')).toBe('approved')
    expect(parseApprovalFilter(['rejected'])).toBe('rejected')
    expect(parseApprovalFilter('unknown')).toBe('pending')
    expect(parseApprovalFilter(undefined)).toBe('pending')
  })

  it('승인 결정과 송금 결과 enum을 안전하게 파싱한다', () => {
    expect(parseApprovalDecision('approved')).toBe('approved')
    expect(parseApprovalDecision('APPROVED')).toBeNull()
    expect(parseApprovalTransferStatus('failed')).toBe('failed')
    expect(parseApprovalTransferStatus('pending')).toBeNull()
  })

  it('오류 문구는 비어 있지 않은 첫 query 값만 사용한다', () => {
    expect(parseOptionalQueryText(['잔액 부족', '무시'])).toBe('잔액 부족')
    expect(parseOptionalQueryText('')).toBeNull()
    expect(parseOptionalQueryText(12)).toBeNull()
  })
})
