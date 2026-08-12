import { beforeEach, describe, expect, it, vi } from 'vitest'

const mocks = vi.hoisted(() => ({
  registerSW: vi.fn(),
}))

vi.mock('virtual:pwa-register', () => ({
  registerSW: mocks.registerSW,
}))

describe('ensureServiceWorkerRegistration', () => {
  beforeEach(() => {
    vi.resetModules()
    vi.clearAllMocks()
    vi.stubEnv('PROD', true)
    Object.defineProperty(navigator, 'serviceWorker', {
      configurable: true,
      value: {
        getRegistration: vi.fn(async () => undefined),
      },
    })
  })

  it('플러그인이 등록한 서비스 워커를 반환한다', async () => {
    const registration = { scope: '/' } as ServiceWorkerRegistration
    mocks.registerSW.mockImplementation((options) => {
      options.onRegisteredSW('/sw.js', registration)
      return vi.fn()
    })
    const { ensureServiceWorkerRegistration } =
      await import('@/lib/service-worker')

    await expect(ensureServiceWorkerRegistration()).resolves.toBe(registration)
  })

  it('등록 실패 후 Promise 캐시를 비워 다음 호출에서 재시도한다', async () => {
    mocks.registerSW.mockImplementationOnce((options) => {
      options.onRegisterError(new Error('registration failed'))
      return vi.fn()
    })
    const { ensureServiceWorkerRegistration } =
      await import('@/lib/service-worker')

    await expect(ensureServiceWorkerRegistration()).rejects.toThrow(
      'registration failed',
    )

    const registration = { scope: '/' } as ServiceWorkerRegistration
    mocks.registerSW.mockImplementationOnce((options) => {
      options.onRegisteredSW('/sw.js', registration)
      return vi.fn()
    })

    await expect(ensureServiceWorkerRegistration()).resolves.toBe(registration)
    expect(mocks.registerSW).toHaveBeenCalledTimes(2)
  })
})
