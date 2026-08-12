import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  clearStorage: vi.fn(),
  deleteCurrentFcmToken: vi.fn(),
  getStoredToken: vi.fn(),
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

import { unregisterPushNotifications } from '@/api/push-session'

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
  })

  it('서버 해제가 실패해도 브라우저 구독과 저장값을 정리한다', async () => {
    vi.spyOn(console, 'warn').mockImplementation(() => undefined)
    mocks.unregisterFcmToken.mockRejectedValue(new Error('network'))

    await expect(unregisterPushNotifications()).resolves.toBeUndefined()

    expect(mocks.deleteCurrentFcmToken).toHaveBeenCalledOnce()
    expect(mocks.clearStorage).toHaveBeenCalledOnce()
  })
})
