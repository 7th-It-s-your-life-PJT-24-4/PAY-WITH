import * as Sentry from '@sentry/vue'
import { describe, expect, it, vi } from 'vitest'

import { initializeSentry } from '@/lib/sentry'

vi.mock('@sentry/vue', () => ({
  init: vi.fn(),
  browserTracingIntegration: vi.fn(() => 'browser-tracing'),
  replayIntegration: vi.fn(() => 'session-replay'),
}))

const app = {} as Parameters<typeof initializeSentry>[0]
const router = {} as Parameters<typeof initializeSentry>[1]

describe('Sentry 초기화', () => {
  it('DSN이 없으면 SDK를 초기화하지 않는다', () => {
    const initialized = initializeSentry(app, router, {
      DEV: true,
      MODE: 'development',
    })

    expect(initialized).toBe(false)
    expect(Sentry.init).not.toHaveBeenCalled()
  })

  it('로컬 환경에서도 오류·로그·트레이싱·리플레이를 활성화한다', () => {
    const initialized = initializeSentry(app, router, {
      DEV: true,
      MODE: 'development',
      VITE_SENTRY_DSN: 'https://public@example.ingest.sentry.io/1',
      VITE_SENTRY_ENVIRONMENT: 'local',
      VITE_SENTRY_TRACES_SAMPLE_RATE: '1',
      VITE_SENTRY_REPLAYS_SESSION_SAMPLE_RATE: '1',
      VITE_SENTRY_REPLAYS_ON_ERROR_SAMPLE_RATE: '1',
    })

    expect(initialized).toBe(true)
    expect(Sentry.browserTracingIntegration).toHaveBeenCalledWith({ router })
    expect(Sentry.replayIntegration).toHaveBeenCalled()
    expect(Sentry.init).toHaveBeenCalledWith(
      expect.objectContaining({
        app,
        dsn: 'https://public@example.ingest.sentry.io/1',
        environment: 'local',
        enableLogs: true,
        integrations: ['browser-tracing', 'session-replay'],
        tracesSampleRate: 1,
        replaysSessionSampleRate: 1,
        replaysOnErrorSampleRate: 1,
      }),
    )
  })

  it('잘못된 샘플링 값은 운영 기본값으로 보정한다', () => {
    initializeSentry(app, router, {
      DEV: false,
      MODE: 'production',
      VITE_SENTRY_DSN: 'https://public@example.ingest.sentry.io/1',
      VITE_SENTRY_TRACES_SAMPLE_RATE: '2',
      VITE_SENTRY_REPLAYS_SESSION_SAMPLE_RATE: '-1',
      VITE_SENTRY_REPLAYS_ON_ERROR_SAMPLE_RATE: 'rate',
    })

    expect(Sentry.init).toHaveBeenLastCalledWith(
      expect.objectContaining({
        tracesSampleRate: 0.1,
        replaysSessionSampleRate: 0.1,
        replaysOnErrorSampleRate: 1,
      }),
    )
  })
})
