import { beforeEach, describe, expect, it, vi } from 'vitest'

import { issueGuardianPairingCode, pairWardWithGuardian } from '@/api/pairing'
import { apiClient } from '@/api/client'

vi.mock('@/api/client', () => ({
  apiClient: {
    post: vi.fn(),
  },
}))

describe('pairing API', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('보호자 인증 코드 발급 응답의 data를 반환한다', async () => {
    const result = {
      code: '72941',
      inviteUrl: 'http://localhost:5173/ward/pairing?code=72941',
      expiresAt: '2026-08-04T14:30:00',
    }
    vi.mocked(apiClient.post).mockResolvedValue({
      success: true,
      data: result,
      message: null,
    })

    await expect(issueGuardianPairingCode()).resolves.toEqual(result)
    expect(apiClient.post).toHaveBeenCalledWith(
      '/guard/pairing',
      expect.anything(),
      undefined,
    )
  })

  it('시니어 연결 요청을 검증 후 전송한다', async () => {
    const result = {
      relationId: 21,
      guardId: 7,
      guardName: '김철수',
      status: 'ACTIVE' as const,
      connectedAt: '2026-08-04T14:26:00',
    }
    vi.mocked(apiClient.post).mockResolvedValue({
      success: true,
      data: result,
      message: null,
    })

    await expect(
      pairWardWithGuardian({ pairingCode: '72941' }),
    ).resolves.toEqual(result)
    expect(apiClient.post).toHaveBeenCalledWith(
      '/ward/pairing',
      expect.anything(),
      { pairingCode: '72941' },
    )
  })

  it('형식이 잘못된 코드는 API 호출 전에 막는다', async () => {
    await expect(
      pairWardWithGuardian({ pairingCode: '12' }),
    ).rejects.toBeDefined()
    expect(apiClient.post).not.toHaveBeenCalled()
  })
})
