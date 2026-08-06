import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/api/client'
import { getGuardTransactionDetail } from '@/api/transactions'

vi.mock('@/api/client', () => ({
  apiClient: {
    get: vi.fn(),
  },
}))

describe('guard transaction API', () => {
  beforeEach(() => vi.clearAllMocks())

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
