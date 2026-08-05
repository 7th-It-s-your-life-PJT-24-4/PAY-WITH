import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/api/client'
import {
  getGuardTransactionHistory,
  getWardTransactionHistory,
} from '@/api/transaction-history'

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

  it('보호자 조회에 wardId와 위험도 조건을 전달한다', async () => {
    await getGuardTransactionHistory(12, {
      riskLevel: 'DANGER',
      page: 0,
      size: 20,
    })

    expect(apiClient.get).toHaveBeenCalledWith(
      '/guard/wards/12/transactions?riskLevel=DANGER&page=0&size=20',
      expect.anything(),
    )
  })
})
