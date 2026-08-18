import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'

import { issueGuardianPairingCode, requestWardPairing } from '@/api/pairing'
import { usePairingStore } from '@/stores/pairing.store'

vi.mock('@/api/pairing', () => ({
  issueGuardianPairingCode: vi.fn(),
  requestWardPairing: vi.fn(),
}))
vi.mock('@/api/error', () => ({
  getApiErrorCode: vi.fn(() => 'PAIRING_002'),
  getApiErrorMessage: vi.fn(
    () => '인증 코드가 유효하지 않거나 만료되었습니다.',
  ),
}))

describe('pairing store', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    setActivePinia(createPinia())
  })

  it('보호자 인증 코드를 생성한다', async () => {
    vi.mocked(issueGuardianPairingCode).mockResolvedValue({
      code: '72941',
      inviteUrl: 'http://localhost:5173/ward/pairing?code=72941',
      expiresAt: '2026-08-04T14:30:00',
    })
    const store = usePairingStore()

    await expect(store.issueCode()).resolves.toBe(true)

    expect(store.status).toBe('CODE_ISSUED')
    expect(store.code).toBe('72941')
    expect(store.inviteUrl).toContain('/ward/pairing')
    expect(store.expiresAt).toBe('2026-08-04T14:30:00')
  })

  it('올바른 코드로 보호자 확인 요청을 생성한다', async () => {
    const store = usePairingStore()
    vi.mocked(requestWardPairing).mockRejectedValueOnce(new Error('invalid'))
    vi.mocked(requestWardPairing).mockResolvedValueOnce('pairing-request-21')

    expect(await store.requestPairing('00000')).toBe(false)
    expect(store.errorCode).toBe('PAIRING_002')
    expect(store.isPaired).toBe(false)

    expect(await store.requestPairing('72941')).toBe(true)
    expect(store.isPaired).toBe(false)
    expect(store.pendingRequestId).toBe('pairing-request-21')
  })

  it('보호자 화면의 연결 상태 감지 결과를 반영한다', () => {
    const store = usePairingStore()

    store.markPaired()

    expect(store.isPaired).toBe(true)
    expect(store.errorMessage).toBe('')
  })
})
