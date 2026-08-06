import { beforeEach, describe, expect, it, vi } from 'vitest'

import { filterBanks, getBanks } from '@/api/banks'
import { apiClient } from '@/api/client'

vi.mock('@/api/client', () => ({
  apiClient: { get: vi.fn(), post: vi.fn() },
}))

describe('bank API', () => {
  beforeEach(() => vi.clearAllMocks())

  it('계좌번호로 조회한 후보 은행 목록을 반환한다', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({
      success: true,
      data: {
        banks: [
          { bankCode: '020', bankName: '우리은행' },
          { bankCode: '081', bankName: '하나은행' },
        ],
      },
      message: null,
    })

    const result = await filterBanks({ accountNo: '12345678' })

    expect(result).toEqual([
      { bankCode: '020', bankName: '우리은행' },
      { bankCode: '081', bankName: '하나은행' },
    ])
    expect(apiClient.post).toHaveBeenCalledWith(
      '/ward/filter-bank',
      expect.anything(),
      { accountNo: '12345678' },
    )
  })

  it('전체 은행 목록을 별도 API로 조회한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: [{ bankCode: '004', bankName: 'KB국민은행' }],
      message: null,
    })

    await expect(getBanks()).resolves.toEqual([
      { bankCode: '004', bankName: 'KB국민은행' },
    ])
    expect(apiClient.get).toHaveBeenCalledWith('/banks', expect.anything())
  })
})
