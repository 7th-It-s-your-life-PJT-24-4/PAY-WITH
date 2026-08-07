import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/api/client'
import { getWardGuardian } from '@/api/ward-guardian'

vi.mock('@/api/client', () => ({
  apiClient: {
    get: vi.fn(),
  },
}))

describe('wardGuardian API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('GET /api/ward/guardian 호출 시 보호자 정보를 반환한다', async () => {
    const guardianData = {
      name: '홍보호',
      phone: '01012345678',
      avatarId: 2,
    }

    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: guardianData,
      message: null,
    })

    await expect(getWardGuardian()).resolves.toEqual(guardianData)
    expect(apiClient.get).toHaveBeenCalledWith(
      '/ward/guardian',
      expect.anything(),
    )
  })
})
