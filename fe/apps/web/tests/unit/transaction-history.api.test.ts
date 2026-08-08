import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/api/client'
import {
  getWardTransactionDetail,
  getWardTransactionHistory,
} from '@/api/transaction-history'
import {
  wardTransactionDetailOptions,
  wardTransactionHistoryOptions,
} from '@/lib/query/transaction-history'

vi.mock('@/api/client', () => ({
  apiClient: { get: vi.fn() },
}))

const emptyPage = {
  transactions: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  hasNext: false,
}

describe('transaction history API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: emptyPage,
      message: null,
    })
  })

  it('피보호자 검색 조건을 query parameter로 전달한다', async () => {
    await getWardTransactionHistory({
      category: 'TRANSFER',
      keyword: '홍길동',
      page: 1,
      size: 20,
    })

    expect(apiClient.get).toHaveBeenCalledWith(
      '/ward/transactions?category=TRANSFER&keyword=%ED%99%8D%EA%B8%B8%EB%8F%99&page=1&size=20',
      expect.anything(),
    )
  })

  it('wardTransactionHistoryOptions select에서 HELD 상태인 거래를 필터링하여 제외한다', () => {
    const options = wardTransactionHistoryOptions({})
    const select = options.select!
    const filtered = select({
      transactions: [
        {
          transactionId: 1,
          type: 'TRANSFER',
          direction: 'OUT',
          title: '김철수',
          amount: 50_000,
          status: 'HELD',
          riskLevel: 'DANGER',
          occurredAt: '2026-08-08T09:00:00',
        },
        {
          transactionId: 2,
          type: 'TRANSFER',
          direction: 'OUT',
          title: '이영희',
          amount: 30_000,
          status: 'COMPLETED',
          riskLevel: 'SAFE',
          occurredAt: '2026-08-08T09:30:00',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
      hasNext: false,
    } as never)

    expect(filtered.transactions).toHaveLength(1)
    expect(filtered.transactions[0]?.transactionId).toBe(2)
    expect(filtered.transactions[0]?.status).toBe('COMPLETED')
  })

  it('피보호자 거래 상세를 요청한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: {
        transactionId: 104,
        type: 'TRANSFER',
        direction: 'OUT',
        status: 'COMPLETED',
        riskLevel: 'DANGER',
        counterpartyName: '김철수',
        bankName: '신한은행',
        accountNo: '110123456789',
        amount: 120_000,
        memo: '생활비',
        occurredAt: '2026-08-05T09:30:00',
        balanceAfter: 380_000,
        riskAnalysis: {
          riskScore: 72,
          summary: '평소보다 큰 금액이 송금됐어요.',
          reasons: ['HIGH_AMOUNT_L2', 'NEW_RECIPIENT'],
        },
      },
      message: null,
    })

    const detail = await getWardTransactionDetail(104)

    expect(apiClient.get).toHaveBeenCalledWith(
      '/ward/transactions/104',
      expect.anything(),
    )
    expect(detail.riskAnalysis?.riskScore).toBe(72)
  })

  it('FDS 룰 코드(송금 및 결제)를 올바른 한글 사유 문구로 변환한다', () => {
    const options = wardTransactionDetailOptions(104)
    const select = options.select!
    const mapped = select({
      transactionId: 104,
      type: 'TRANSFER',
      direction: 'OUT',
      status: 'COMPLETED',
      riskLevel: 'DANGER',
      counterpartyName: '김철수',
      bankName: '신한은행',
      accountNo: '110123456789',
      amount: 120_000,
      memo: '생활비',
      occurredAt: '2026-08-05T09:30:00',
      balanceAfter: 380_000,
      riskAnalysis: {
        riskScore: 85,
        summary: null,
        reasons: [
          'DIVISION_TRANSFER',
          'REPEATED',
          'PAY_HIGH_AMOUNT_L3',
          'PAY_PENDING_APPROVAL',
          'PAY_SPLIT_PAYMENT',
        ],
      },
    })

    expect(mapped.riskReasons).toEqual([
      '짧은 시간 안에 여러 계좌로 나누어 송금했어요.',
      '같은 계좌로 반복해서 송금했어요.',
      '평소보다 매우 큰 금액을 결제했어요.',
      '승인 대기 중인 송금이 있는 상태에서 결제했어요.',
      '짧은 시간 안에 여러 번 나누어 결제했어요.',
    ])
  })

  it('FAILED 및 CANCELED 상태의 거래에 적절한 기본 안내 요약 문구를 생성한다', () => {
    const options = wardTransactionDetailOptions(105)
    const select = options.select!
    const failedMapped = select({
      transactionId: 105,
      type: 'TRANSFER',
      direction: 'OUT',
      status: 'FAILED',
      riskLevel: null,
      counterpartyName: '김철수',
      bankName: '신한은행',
      accountNo: '110123456789',
      amount: 120_000,
      memo: null,
      occurredAt: '2026-08-05T09:30:00',
      balanceAfter: null,
      riskAnalysis: null,
    })

    expect(failedMapped.riskSummary).toBe(
      '잔액 부족 등으로 거래가 완료되지 않았어요.',
    )
  })
})
