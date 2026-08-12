import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  deleteToken: vi.fn(),
  getApp: vi.fn(),
  getApps: vi.fn(),
  getMessaging: vi.fn(),
  getToken: vi.fn(),
  initializeApp: vi.fn(),
  isSupported: vi.fn(),
  ensureServiceWorkerRegistration: vi.fn(),
}))

vi.mock('firebase/app', () => ({
  getApp: mocks.getApp,
  getApps: mocks.getApps,
  initializeApp: mocks.initializeApp,
}))

vi.mock('firebase/messaging', () => ({
  deleteToken: mocks.deleteToken,
  getMessaging: mocks.getMessaging,
  getToken: mocks.getToken,
  isSupported: mocks.isSupported,
}))

vi.mock('@/lib/service-worker', () => ({
  ensureServiceWorkerRegistration: mocks.ensureServiceWorkerRegistration,
}))

describe('Firebase Messaging 초기화', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.resetAllMocks()
    vi.stubEnv('VITE_FIREBASE_API_KEY', 'api-key')
    vi.stubEnv('VITE_FIREBASE_AUTH_DOMAIN', 'project-id.firebaseapp.com')
    vi.stubEnv('VITE_FIREBASE_PROJECT_ID', 'project-id')
    vi.stubEnv('VITE_FIREBASE_MESSAGING_SENDER_ID', 'sender-id')
    vi.stubEnv('VITE_FIREBASE_APP_ID', 'app-id')
    vi.stubEnv('VITE_FIREBASE_VAPID_KEY', 'vapid-key')
    Object.defineProperty(navigator, 'serviceWorker', {
      configurable: true,
      value: {},
    })
    mocks.getApps.mockReturnValue([])
    mocks.initializeApp.mockReturnValue({ name: 'pay-with' })
    mocks.getMessaging.mockReturnValue({ app: { name: 'pay-with' } })
    mocks.getToken.mockResolvedValue('device-token')
    mocks.deleteToken.mockResolvedValue(true)
    mocks.ensureServiceWorkerRegistration.mockResolvedValue({ scope: '/' })
  })

  it('전달받은 Firebase Web config로 초기화한다', async () => {
    mocks.isSupported.mockResolvedValue(true)
    const { getFirebaseMessaging, hasFirebaseConfig } =
      await import('@/lib/firebase')

    await expect(getFirebaseMessaging()).resolves.toBeTruthy()

    expect(hasFirebaseConfig()).toBe(true)
    expect(mocks.initializeApp).toHaveBeenCalledWith({
      apiKey: 'api-key',
      appId: 'app-id',
      authDomain: 'project-id.firebaseapp.com',
      messagingSenderId: 'sender-id',
      projectId: 'project-id',
    })
  })

  it('초기화가 실패하면 다음 호출에서 다시 시도한다', async () => {
    mocks.isSupported
      .mockRejectedValueOnce(new Error('temporary failure'))
      .mockResolvedValueOnce(true)
    const { getFirebaseMessaging } = await import('@/lib/firebase')

    await expect(getFirebaseMessaging()).rejects.toThrow('temporary failure')
    await expect(getFirebaseMessaging()).resolves.toBeTruthy()

    expect(mocks.isSupported).toHaveBeenCalledTimes(2)
  })

  it('커스텀 서비스 워커를 Messaging에 연결한 뒤 토큰을 삭제한다', async () => {
    mocks.isSupported.mockResolvedValue(true)
    const { deleteCurrentFcmToken } = await import('@/lib/firebase')

    await deleteCurrentFcmToken()

    expect(mocks.getToken).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        serviceWorkerRegistration: { scope: '/' },
        vapidKey: 'vapid-key',
      }),
    )
    expect(mocks.deleteToken).toHaveBeenCalledOnce()
    expect(mocks.getToken.mock.invocationCallOrder[0]).toBeLessThan(
      mocks.deleteToken.mock.invocationCallOrder[0] ?? 0,
    )
  })
})
