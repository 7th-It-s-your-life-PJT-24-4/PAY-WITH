import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  ensureServiceWorkerRegistration: vi.fn(),
  getCurrentFcmToken: vi.fn(),
  getFirebaseMessaging: vi.fn(),
  isPushTokenSessionSynced: vi.fn(),
  markPushTokenSessionSynced: vi.fn(),
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

vi.mock('@/lib/push-token-sync-state', () => ({
  isPushTokenSessionSynced: mocks.isPushTokenSessionSynced,
  markPushTokenSessionSynced: mocks.markPushTokenSessionSynced,
}))

vi.mock('@/lib/service-worker', () => ({
  ensureServiceWorkerRegistration: mocks.ensureServiceWorkerRegistration,
}))

describe('usePushNotification', () => {
  beforeEach(() => {
    vi.resetModules()
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
    mocks.isPushTokenSessionSynced.mockReturnValue(false)
  })

  it('권한 허용 뒤 토큰을 등록하고 수신·재동기화 리스너를 연결한다', async () => {
    const { startPushNotifications, usePushNotification } =
      await import('@/composables/usePushNotification')
    let foregroundHandler: ((payload: unknown) => void) | undefined
    mocks.onMessage.mockImplementation((_messaging, handler) => {
      foregroundHandler = handler
    })
    const windowListenerSpy = vi.spyOn(window, 'addEventListener')
    const documentListenerSpy = vi.spyOn(document, 'addEventListener')
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
    expect(mocks.markPushTokenSessionSynced).toHaveBeenCalledWith(
      '7:device-token',
    )
    expect(windowListenerSpy).toHaveBeenCalledWith(
      'focus',
      expect.any(Function),
    )
    expect(documentListenerSpy).toHaveBeenCalledWith(
      'visibilitychange',
      expect.any(Function),
    )

    foregroundHandler?.({
      data: {
        type: 'ANOMALY',
        refType: 'TRANSACTION',
        refId: '8',
      },
      notification: { title: '이상 거래', body: '확인이 필요합니다.' },
    })

    expect(usePushNotification().foregroundNotification.value).toMatchObject({
      title: '이상 거래',
      body: '확인이 필요합니다.',
      path: '/guard?source=push',
    })
  })

  it('서비스 워커가 없으면 비활성화하고 초기화를 다시 시도할 수 있다', async () => {
    mocks.ensureServiceWorkerRegistration.mockResolvedValue(null)
    const { startPushNotifications, usePushNotification } =
      await import('@/composables/usePushNotification')
    const router = {
      afterEach: vi.fn(),
      push: vi.fn(),
    }

    await startPushNotifications(router as never)
    await startPushNotifications(router as never)

    expect(usePushNotification().availability.value).toBe('disabled')
    expect(mocks.ensureServiceWorkerRegistration).toHaveBeenCalledTimes(2)
    expect(mocks.getFirebaseMessaging).not.toHaveBeenCalled()
  })

  it('서비스 워커 등록 실패 후 다음 초기화 호출에서 재시도한다', async () => {
    vi.spyOn(console, 'warn').mockImplementation(() => undefined)
    mocks.ensureServiceWorkerRegistration.mockRejectedValueOnce(
      new Error('registration failed'),
    )
    const { startPushNotifications, usePushNotification } =
      await import('@/composables/usePushNotification')
    const router = {
      afterEach: vi.fn(),
      push: vi.fn(),
    }

    await startPushNotifications(router as never)
    expect(usePushNotification().availability.value).toBe('unsupported')

    await startPushNotifications(router as never)

    expect(mocks.ensureServiceWorkerRegistration).toHaveBeenCalledTimes(2)
    expect(mocks.getFirebaseMessaging).toHaveBeenCalledOnce()
    expect(usePushNotification().availability.value).toBe('available')
  })
})
