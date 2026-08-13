import * as Sentry from '@sentry/vue'
import type { App } from 'vue'
import type { Router } from 'vue-router'

interface SentryEnvironment {
  readonly DEV: boolean
  readonly MODE: string
  readonly VITE_SENTRY_DSN?: string
  readonly VITE_SENTRY_ENVIRONMENT?: string
  readonly VITE_SENTRY_TRACES_SAMPLE_RATE?: string
  readonly VITE_SENTRY_REPLAYS_SESSION_SAMPLE_RATE?: string
  readonly VITE_SENTRY_REPLAYS_ON_ERROR_SAMPLE_RATE?: string
}

function parseSampleRate(value: string | undefined, fallback: number) {
  if (value === undefined || value.trim() === '') return fallback

  const parsed = Number(value)
  return Number.isFinite(parsed) && parsed >= 0 && parsed <= 1
    ? parsed
    : fallback
}

export function initializeSentry(
  app: App,
  router: Router,
  environment: SentryEnvironment = import.meta.env,
) {
  const dsn = environment.VITE_SENTRY_DSN?.trim()
  if (!dsn) return false

  const defaultSessionSampleRate = environment.DEV ? 1 : 0.1

  Sentry.init({
    app,
    dsn,
    environment:
      environment.VITE_SENTRY_ENVIRONMENT?.trim() || environment.MODE,
    integrations: [
      Sentry.browserTracingIntegration({ router }),
      Sentry.replayIntegration(),
    ],
    enableLogs: true,
    tracesSampleRate: parseSampleRate(
      environment.VITE_SENTRY_TRACES_SAMPLE_RATE,
      environment.DEV ? 1 : 0.1,
    ),
    tracePropagationTargets: ['localhost', /^\/api(?:\/|$)/],
    replaysSessionSampleRate: parseSampleRate(
      environment.VITE_SENTRY_REPLAYS_SESSION_SAMPLE_RATE,
      defaultSessionSampleRate,
    ),
    replaysOnErrorSampleRate: parseSampleRate(
      environment.VITE_SENTRY_REPLAYS_ON_ERROR_SAMPLE_RATE,
      1,
    ),
  })

  return true
}
