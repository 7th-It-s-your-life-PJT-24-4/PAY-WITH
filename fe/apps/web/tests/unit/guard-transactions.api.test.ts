import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/api/client'
import {
  getGuardTransactionDetail,
  getGuardTransactionHistory,
} from '@/api/guard-transactions'

vi.mock('@/api/client', () => ({
  apiClient: { get: vi.fn() },
}))

describe('guard transactions api', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('위험도와 페이지 크기를 포함해 거래내역을 조회한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: {
        transactions: [],
        page: 0,
        size: 100,
        totalElements: 0,
        totalPages: 0,
        hasNext: false,
      },
      message: null,
    })

    await getGuardTransactionHistory(12, 'DANGER')

    expect(apiClient.get).toHaveBeenCalledWith(
      '/guard/wards/12/transactions?page=0&size=100&riskLevel=DANGER',
      expect.anything(),
    )
  })

  it('숫자 피보호자·거래 ID로 상세를 조회한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: { transactionId: 141 },
      message: null,
    })

    await getGuardTransactionDetail(12, 141)

    expect(apiClient.get).toHaveBeenCalledWith(
      '/guard/wards/12/transactions/141',
      expect.anything(),
    )
  })
})
