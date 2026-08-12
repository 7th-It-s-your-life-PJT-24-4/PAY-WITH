import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  ensureServiceWorkerRegistration: vi.fn(),
  getCurrentFcmToken: vi.fn(),
  getFirebaseMessaging: vi.fn(),
  onMessage: vi.fn(),
  registerFcmToken: vi.fn(),
  setStoredToken: vi.fn(),
}))

vi.mock('firebase/messaging', () => ({
  onMessage: mocks.onMessage,
}))

vi.mock('@/api/fcm-token', () => ({
  registerFcmToken: mocks.registerFcmToken,
}))

vi.mock('@/api/token-storage', () => ({
  getUserIdFromAccessToken: () => 7,
  tokenStorage: {
    getAccessToken: () => 'access-token',
  },
}))

vi.mock('@/lib/firebase', () => ({
  getCurrentFcmToken: mocks.getCurrentFcmToken,
  getFirebaseMessaging: mocks.getFirebaseMessaging,
  hasFirebaseConfig: () => true,
}))

vi.mock('@/lib/push-token-storage', () => ({
  pushTokenStorage: {
    set: mocks.setStoredToken,
  },
}))

vi.mock('@/lib/service-worker', () => ({
  ensureServiceWorkerRegistration: mocks.ensureServiceWorkerRegistration,
}))

import {
  startPushNotifications,
  usePushNotification,
} from '@/composables/usePushNotification'

describe('usePushNotification', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    const notification = {
      permission: 'default' as NotificationPermission,
      requestPermission: vi.fn(async () => {
        notification.permission = 'granted'
        return 'granted' as const
      }),
    }
    vi.stubGlobal('Notification', notification)
    Object.defineProperty(navigator, 'serviceWorker', {
      configurable: true,
      value: { addEventListener: vi.fn() },
    })
    mocks.ensureServiceWorkerRegistration.mockResolvedValue({ scope: '/' })
    mocks.getFirebaseMessaging.mockResolvedValue({ app: {} })
    mocks.getCurrentFcmToken.mockResolvedValue('device-token')
    mocks.registerFcmToken.mockResolvedValue(undefined)
  })

  it('사용자 허용 뒤 현재 사용자 토큰을 서버에 등록하고 로컬에 보관한다', async () => {
    const router = {
      afterEach: vi.fn(),
      push: vi.fn(),
    }
    await startPushNotifications(router as never)

    await usePushNotification().requestPermission()

    expect(mocks.registerFcmToken).toHaveBeenCalledWith('device-token')
    expect(mocks.setStoredToken).toHaveBeenCalledWith({
      token: 'device-token',
      userId: 7,
    })
  })
})
