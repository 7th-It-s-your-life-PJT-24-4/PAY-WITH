import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/api/client'
import { getWardApprovalDetail, getWardHome } from '@/api/home'
import { getWardWallet } from '@/api/wallet'

vi.mock('@/api/client', () => ({
  apiClient: { get: vi.fn() },
}))

describe('ward home API', () => {
  beforeEach(() => vi.clearAllMocks())

  it('인증된 피보호자의 홈을 조회한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: { userName: '김시니어' },
      message: null,
    })

    await getWardHome()

    expect(apiClient.get).toHaveBeenCalledWith('/ward/home', expect.anything())
  })

  it('approvalId로 승인 대기 상세를 조회한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: { approvalId: 7 },
      message: null,
    })

    await getWardApprovalDetail(7)

    expect(apiClient.get).toHaveBeenCalledWith(
      '/ward/approval-requests/7',
      expect.anything(),
    )
  })

  it('홈을 거치지 않아도 지갑 잔액을 직접 조회한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: { walletId: 9207, balance: 500_000 },
      message: null,
    })

    await getWardWallet()

    expect(apiClient.get).toHaveBeenCalledWith(
      '/ward/wallet',
      expect.anything(),
    )
  })
})
