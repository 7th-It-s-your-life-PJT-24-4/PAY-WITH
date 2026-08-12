import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'

import TransactionSummaryCard from '@/pages/ward/history/-components/TransactionSummaryCard.vue'
import type { WardTransaction } from '@/types/transaction'

const sampleTransferTransaction: WardTransaction = {
  transactionId: 101,
  type: 'TRANSFER',
  direction: 'OUT',
  title: '김철수',
  amount: 50_000,
  balanceAfter: 450_000,
  occurredAt: '2026-08-12T10:30:00',
  methodLabel: '간편 송금',
  memo: '생일 축하금',
  status: 'COMPLETED',
  riskLevel: null,
  riskScore: 0,
  riskSummary: '',
  riskReasons: [],
  transfer: {
    holderName: '김철수',
    bankName: '신한은행',
    accountNo: '110-123-456789',
  },
}

const samplePaymentTransaction: WardTransaction = {
  transactionId: 102,
  type: 'PAYMENT',
  direction: 'OUT',
  title: '스타벅스 강남점',
  amount: 6_500,
  balanceAfter: 443_500,
  occurredAt: '2026-08-12T11:00:00',
  methodLabel: '바코드 결제',
  memo: null,
  status: 'COMPLETED',
  riskLevel: null,
  riskScore: 0,
  riskSummary: '',
  riskReasons: [],
}

const sampleChargeTransaction: WardTransaction = {
  transactionId: 103,
  type: 'CHARGE',
  direction: 'IN',
  title: 'KB국민은행',
  amount: 100_000,
  balanceAfter: 543_500,
  occurredAt: '2026-08-12T09:00:00',
  methodLabel: '연결 계좌 입금',
  memo: null,
  status: 'COMPLETED',
  riskLevel: null,
  riskScore: 0,
  riskSummary: '',
  riskReasons: [],
}

const sampleBlockedTransaction: WardTransaction = {
  transactionId: 104,
  type: 'TRANSFER',
  direction: 'OUT',
  title: '의심 수취인',
  amount: 300_000,
  balanceAfter: null,
  occurredAt: '2026-08-12T12:00:00',
  methodLabel: '간편 송금',
  memo: null,
  status: 'BLOCKED',
  riskLevel: 'DANGER',
  riskScore: 92,
  riskSummary: '이상거래 감지',
  riskReasons: ['SUSPICIOUS_MEMO'],
}

describe('TransactionSummaryCard', () => {
  it('송금 거래 상세를 상단 Hero 영역과 1열 리스트로 정확하게 렌더링한다', () => {
    const wrapper = mount(TransactionSummaryCard, {
      props: {
        transaction: sampleTransferTransaction,
      },
    })

    // Hero 영역 검증
    expect(wrapper.get('#transaction-summary-title').text()).toBe('김철수')
    expect(wrapper.text()).toContain('신한은행')
    expect(wrapper.text()).toContain('110-123-456789')
    expect(wrapper.text()).toContain('-50,000원')

    // 1열 상세 목록 라벨 및 값 검증
    expect(wrapper.text()).toContain('거래 종류')
    expect(wrapper.text()).toContain('보낸 돈')
    expect(wrapper.text()).toContain('받는 분')
    expect(wrapper.text()).toContain('거래 일시')
    expect(wrapper.text()).toContain('거래 수단')
    expect(wrapper.text()).toContain('간편 송금')
    expect(wrapper.text()).toContain('메모')
    expect(wrapper.text()).toContain('생일 축하금')
    expect(wrapper.text()).toContain('거래 후 잔액')
    expect(wrapper.text()).toContain('450,000원')
  })

  it('결제 거래일 때 사용처 라벨과 결제 금액을 올바르게 표시한다', () => {
    const wrapper = mount(TransactionSummaryCard, {
      props: {
        transaction: samplePaymentTransaction,
      },
    })

    expect(wrapper.get('#transaction-summary-title').text()).toBe(
      '스타벅스 강남점',
    )
    expect(wrapper.text()).toContain('사용처')
    expect(wrapper.text()).toContain('결제')
    expect(wrapper.text()).toContain('바코드 결제')
    expect(wrapper.text()).toContain('-6,500원')
    expect(wrapper.text()).not.toContain('메모')
  })

  it('충전 거래일 때 입금(+) 금액과 충전 계좌를 표시한다', () => {
    const wrapper = mount(TransactionSummaryCard, {
      props: {
        transaction: sampleChargeTransaction,
      },
    })

    expect(wrapper.get('#transaction-summary-title').text()).toBe('KB국민은행')
    expect(wrapper.text()).toContain('충전')
    expect(wrapper.text()).toContain('+100,000원')
    expect(wrapper.text()).toContain('충전 계좌')
  })

  it('차단/취소/실패 거래일 때 취소선 스타일과 안내를 적용한다', () => {
    const wrapper = mount(TransactionSummaryCard, {
      props: {
        transaction: sampleBlockedTransaction,
      },
    })

    expect(wrapper.get('#transaction-summary-title').text()).toBe('의심 수취인')
    expect(wrapper.text()).toContain('300,000원')
    const amountEl = wrapper.get('.type-amount')
    expect(amountEl.classes()).toContain('line-through')
    expect(wrapper.text()).toContain('거래 후 잔액')
    expect(wrapper.text()).toContain('-')
  })
})
