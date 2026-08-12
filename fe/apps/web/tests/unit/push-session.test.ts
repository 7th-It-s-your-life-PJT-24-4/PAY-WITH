import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  clearStorage: vi.fn(),
  deleteCurrentFcmToken: vi.fn(),
  getStoredToken: vi.fn(),
  resetPushTokenSyncState: vi.fn(),
  unregisterFcmToken: vi.fn(),
}))

vi.mock('@/api/fcm-token', () => ({
  unregisterFcmToken: mocks.unregisterFcmToken,
}))

vi.mock('@/lib/firebase', () => ({
  deleteCurrentFcmToken: mocks.deleteCurrentFcmToken,
}))

vi.mock('@/lib/push-token-storage', () => ({
  pushTokenStorage: {
    clear: mocks.clearStorage,
    get: mocks.getStoredToken,
  },
}))

vi.mock('@/lib/push-token-sync-state', () => ({
  resetPushTokenSyncState: mocks.resetPushTokenSyncState,
}))

import {
  clearLocalPushSubscription,
  unregisterPushNotifications,
} from '@/api/push-session'

describe('푸시 알림 로그아웃 정리', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    mocks.getStoredToken.mockReturnValue({ token: 'device-token', userId: 7 })
    mocks.unregisterFcmToken.mockResolvedValue(undefined)
    mocks.deleteCurrentFcmToken.mockResolvedValue(undefined)
  })

  it('서버 토큰을 먼저 해제하고 브라우저 구독과 저장값을 정리한다', async () => {
    const order: string[] = []
    mocks.unregisterFcmToken.mockImplementation(async () => {
      order.push('server')
    })
    mocks.deleteCurrentFcmToken.mockImplementation(async () => {
      order.push('browser')
    })
    mocks.clearStorage.mockImplementation(() => {
      order.push('storage')
    })

    await unregisterPushNotifications()

    expect(order).toEqual(['server', 'browser', 'storage'])
    expect(mocks.unregisterFcmToken).toHaveBeenCalledWith('device-token')
    expect(mocks.resetPushTokenSyncState).toHaveBeenCalledOnce()
  })

  it('서버 해제가 실패해도 브라우저 구독과 저장값을 정리한다', async () => {
    vi.spyOn(console, 'warn').mockImplementation(() => undefined)
    mocks.unregisterFcmToken.mockRejectedValue(new Error('network'))

    await expect(unregisterPushNotifications()).resolves.toBeUndefined()

    expect(mocks.deleteCurrentFcmToken).toHaveBeenCalledOnce()
    expect(mocks.clearStorage).toHaveBeenCalledOnce()
    expect(mocks.resetPushTokenSyncState).toHaveBeenCalledOnce()
  })

  it('브라우저 토큰 삭제가 실패해도 다음 로그인용 동기화 상태를 초기화한다', async () => {
    vi.spyOn(console, 'warn').mockImplementation(() => undefined)
    mocks.deleteCurrentFcmToken.mockRejectedValue(new Error('firebase'))

    await expect(unregisterPushNotifications()).resolves.toBeUndefined()

    expect(mocks.clearStorage).toHaveBeenCalledOnce()
    expect(mocks.resetPushTokenSyncState).toHaveBeenCalledOnce()
  })

  it('세션 만료 시 인증이 필요한 서버 요청 없이 로컬 구독만 정리한다', async () => {
    await clearLocalPushSubscription()

    expect(mocks.unregisterFcmToken).not.toHaveBeenCalled()
    expect(mocks.deleteCurrentFcmToken).toHaveBeenCalledOnce()
    expect(mocks.clearStorage).toHaveBeenCalledOnce()
    expect(mocks.resetPushTokenSyncState).toHaveBeenCalledOnce()
  })
})
