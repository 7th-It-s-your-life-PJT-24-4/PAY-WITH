import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiClient } from '@/api/client'
import { getGuardHome } from '@/api/guard-home'

vi.mock('@/api/client', () => ({
  apiClient: { get: vi.fn() },
}))

describe('guard home API', () => {
  beforeEach(() => vi.clearAllMocks())

  it('보호자의 연결된 시니어 목록과 기본 홈 정보를 조회한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: { wards: [], selectedWard: null },
      message: null,
    })

    await getGuardHome()

    expect(apiClient.get).toHaveBeenCalledWith('/guard', expect.anything())
  })

  it('선택한 시니어의 홈 정보를 조회한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: { wards: [], selectedWard: null },
      message: null,
    })

    await getGuardHome(12)

    expect(apiClient.get).toHaveBeenCalledWith(
      '/guard?wardId=12',
      expect.anything(),
    )
  })
})
