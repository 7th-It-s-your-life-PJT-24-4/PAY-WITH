import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  confirmPairingRequest,
  getPendingPairingRequest,
  getWardPairingRequestStatus,
  issueGuardianPairingCode,
  requestWardPairing,
  unpairGuardianWard,
} from '@/api/pairing'
import { apiClient } from '@/api/client'

vi.mock('@/api/client', () => ({
  apiClient: {
    delete: vi.fn(),
    get: vi.fn(),
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

  it('시니어 확인 요청을 검증 후 전송한다', async () => {
    vi.mocked(apiClient.post).mockResolvedValue({
      success: true,
      data: { requestId: 'pairing-request-21' },
      message: null,
    })

    await expect(requestWardPairing({ pairingCode: '72941' })).resolves.toBe(
      'pairing-request-21',
    )
    expect(apiClient.post).toHaveBeenCalledWith(
      '/ward/pairing/request',
      expect.anything(),
      { pairingCode: '72941' },
    )
  })

  it('형식이 잘못된 코드는 API 호출 전에 막는다', async () => {
    await expect(
      requestWardPairing({ pairingCode: '12' }),
    ).rejects.toBeDefined()
    expect(apiClient.post).not.toHaveBeenCalled()
  })

  it('시니어가 확인 요청 상태를 조회한다', async () => {
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: { status: 'PENDING' },
      message: null,
    })

    await expect(
      getWardPairingRequestStatus('pairing-request-21'),
    ).resolves.toBe('PENDING')
    expect(apiClient.get).toHaveBeenCalledWith(
      '/ward/pairing/request/pairing-request-21/status',
      expect.anything(),
    )
  })

  it('보호자가 대기 요청을 조회하고 수락한다', async () => {
    const pairing = {
      relationId: 21,
      guardId: 7,
      guardName: '김철수',
      status: 'ACTIVE' as const,
      connectedAt: '2026-08-04T14:26:00',
    }
    vi.mocked(apiClient.get).mockResolvedValue({
      success: true,
      data: {
        requestId: 'pairing-request-21',
        wardName: '이영희',
        wardPhoneMasked: '010-****-1234',
      },
      message: null,
    })
    vi.mocked(apiClient.post).mockResolvedValue({
      success: true,
      data: pairing,
      message: null,
    })

    await expect(getPendingPairingRequest()).resolves.toMatchObject({
      wardName: '이영희',
    })
    await expect(confirmPairingRequest('pairing-request-21')).resolves.toEqual(
      pairing,
    )
    expect(apiClient.get).toHaveBeenCalledWith(
      '/guard/pairing/pending-request',
      expect.anything(),
    )
    expect(apiClient.post).toHaveBeenCalledWith(
      '/guard/pairing/request/pairing-request-21/confirm',
      expect.anything(),
      undefined,
    )
  })

  it('보호자가 시니어 연결을 해제한다', async () => {
    vi.mocked(apiClient.delete).mockResolvedValue({
      success: true,
      data: null,
      message: null,
    })

    await expect(unpairGuardianWard(12)).resolves.toBeUndefined()
    expect(apiClient.delete).toHaveBeenCalledWith(
      '/guard/pairing/12',
      expect.anything(),
    )
  })
})
