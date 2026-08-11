import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/api/client'
import {
  getGuardTransactionDetail,
  getGuardTransactionHistory,
} from '@/api/transactions'
import { guardTransactionHistoryOptions } from '@/lib/query/guard/transaction'

vi.mock('@/api/client', () => ({
  apiClient: {
    get: vi.fn(),
  },
}))

describe('guard transaction API', () => {
  beforeEach(() => vi.clearAllMocks())

  it('피보호자와 위험도 조건으로 보호자용 거래 목록을 조회한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: { transactions: [] },
      message: null,
    })

    await getGuardTransactionHistory(12, {
      riskLevel: 'DANGER',
      page: 0,
      size: 100,
    })

    expect(apiClient.get).toHaveBeenCalledWith(
      '/guard/wards/12/transactions?riskLevel=DANGER&page=0&size=100',
      expect.anything(),
    )
  })

  it('guardTransactionHistoryOptions select에서 완료된 거래만 남긴다', () => {
    const options = guardTransactionHistoryOptions(12, {})
    const select = options.select!
    const filtered = select({
      transactions: [
        {
          transactionId: 101,
          type: 'TRANSFER',
          direction: 'OUT',
          status: 'HELD',
          riskLevel: 'DANGER',
          amount: 50_000,
          occurredAt: '2026-08-08T09:00:00',
          title: '김철수',
        },
        {
          transactionId: 102,
          type: 'TRANSFER',
          direction: 'OUT',
          status: 'COMPLETED',
          riskLevel: 'SAFE',
          amount: 30_000,
          occurredAt: '2026-08-08T09:30:00',
          title: '이영희',
        },
        {
          transactionId: 103,
          type: 'TRANSFER',
          direction: 'OUT',
          status: 'FAILED',
          riskLevel: 'DANGER',
          amount: 20_000,
          occurredAt: '2026-08-08T10:00:00',
          title: '박실패',
        },
        {
          transactionId: 104,
          type: 'TRANSFER',
          direction: 'OUT',
          status: 'CANCELED',
          riskLevel: 'CAUTION',
          amount: 10_000,
          occurredAt: '2026-08-08T10:30:00',
          title: '최취소',
        },
      ],
      page: 0,
      size: 100,
      totalElements: 4,
      totalPages: 1,
      hasNext: false,
    } as never)

    expect(filtered.transactions).toHaveLength(1)
    expect(filtered.transactions[0]?.transactionId).toBe(102)
    expect(filtered.transactions[0]?.status).toBe('COMPLETED')
  })

  it('피보호자와 거래 ID로 보호자용 거래 상세를 조회한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: { transactionId: 41 },
      message: null,
    })

    await getGuardTransactionDetail(12, 41)

    expect(apiClient.get).toHaveBeenCalledWith(
      '/guard/wards/12/transactions/41',
      expect.anything(),
    )
  })
})
