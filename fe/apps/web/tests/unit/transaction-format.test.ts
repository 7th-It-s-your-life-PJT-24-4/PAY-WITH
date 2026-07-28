import { describe, expect, it } from 'vitest'

import {
  formatTransactionAmount,
  formatTransactionBalance,
  getTransactionRiskLabel,
  getTransactionTypeLabel,
} from '@/pages/ward/history/-utils/transaction-format'

describe('transaction-format', () => {
  it('받은 돈과 나간 돈의 금액 부호를 구분한다', () => {
    expect(formatTransactionAmount(200_000, 'CREDIT')).toBe('+200,000원')
    expect(formatTransactionAmount(45_200, 'DEBIT')).toBe('-45,200원')
  })

  it('거래 후 잔액을 부호 없이 표시한다', () => {
    expect(formatTransactionBalance(1_454_800)).toBe('1,454,800원')
  })

  it('결제와 송금 방향을 시니어용 문구로 표시한다', () => {
    expect(getTransactionTypeLabel('PAYMENT', 'DEBIT')).toBe('결제')
    expect(getTransactionTypeLabel('TRANSFER', 'CREDIT')).toBe('받은 돈')
    expect(getTransactionTypeLabel('TRANSFER', 'DEBIT')).toBe('보낸 돈')
  })

  it('위험 판단과 보호자 차단 결과를 구분한다', () => {
    expect(getTransactionRiskLabel('DANGER', 'COMPLETED')).toBe('위험')
    expect(getTransactionRiskLabel('DANGER', 'BLOCKED')).toBe('거래 차단됨')
  })
})
