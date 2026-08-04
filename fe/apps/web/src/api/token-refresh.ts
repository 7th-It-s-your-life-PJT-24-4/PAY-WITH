import ky from 'ky'

import {
  expireAuthenticationSession,
  markSessionActive,
} from '@/api/auth-session'
import { tokenStorage } from '@/api/token-storage'
import { getAccessTokenExpiresAt } from '@/api/token-storage'
import {
  refreshTokenRequestSchema,
  tokenResponseSchema,
} from '@/schemas/auth.schema'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

let refreshPromise: Promise<string> | null = null
let refreshTimer: ReturnType<typeof globalThis.setTimeout> | null = null
let schedulerStarted = false
const refreshLeewayMs = 60_000

async function requestNewAccessToken(refreshToken: string): Promise<string> {
  // 인증 훅이 걸린 apiClient 대신 순수 ky 인스턴스를 써서 재귀 호출을 막는다.
  const body = refreshTokenRequestSchema.parse({ refreshToken })
  const response = await ky.post(`${API_BASE_URL}/auth/refresh`, { json: body })
  const json: unknown = await response.json()
  const { data } = tokenResponseSchema.parse(json)

  tokenStorage.setTokens(data.accessToken, data.refreshToken)
  markSessionActive()
  scheduleAccessTokenRefresh()
  return data.accessToken
}

export function scheduleAccessTokenRefresh(): void {
  if (refreshTimer) globalThis.clearTimeout(refreshTimer)
  refreshTimer = null

  const accessToken = tokenStorage.getAccessToken()
  if (!accessToken || !tokenStorage.getRefreshToken()) return

  const expiresAt = getAccessTokenExpiresAt(accessToken)
  if (expiresAt === null) return

  const delay = Math.max(0, expiresAt - Date.now() - refreshLeewayMs)
  refreshTimer = globalThis.setTimeout(() => {
    void refreshAccessToken().catch(() => expireAuthenticationSession())
  }, delay)
}

export function startAccessTokenRefreshScheduler(): void {
  scheduleAccessTokenRefresh()
  if (schedulerStarted || typeof document === 'undefined') return

  schedulerStarted = true
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState === 'visible') scheduleAccessTokenRefresh()
  })
}

// 동시에 여러 요청이 401을 받아도 refresh 호출은 한 번만 나가도록 in-flight 요청을 공유한다.
export async function refreshAccessToken(): Promise<string> {
  const refreshToken = tokenStorage.getRefreshToken()

  if (!refreshToken) {
    throw new Error('저장된 refresh token이 없습니다.')
  }

  if (!refreshPromise) {
    refreshPromise = requestNewAccessToken(refreshToken).finally(() => {
      refreshPromise = null
    })
  }

  return await refreshPromise
}
