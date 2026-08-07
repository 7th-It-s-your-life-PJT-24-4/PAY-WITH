import ky from 'ky'

import {
  expireAuthenticationSession,
  markSessionActive,
} from '@/api/auth-session'
import { isUnauthorizedApiError } from '@/api/error'
import { getAccessTokenExpiresAt, tokenStorage } from '@/api/token-storage'
import {
  refreshTokenRequestSchema,
  tokenResponseSchema,
} from '@/schemas/auth.schema'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

let refreshPromise: Promise<string> | null = null
let refreshTimer: ReturnType<typeof globalThis.setTimeout> | null = null
let checkInterval: ReturnType<typeof globalThis.setInterval> | null = null
let scheduledExpiresAt: number | null = null
let schedulerStarted = false
const refreshLeewayMs = 60_000
const refreshRetryDelayMs = 30_000
const periodicCheckIntervalMs = 30_000

function getAdvancedSessionAccessToken(refreshToken: string): string | null {
  const currentRefreshToken = tokenStorage.getRefreshToken()
  const currentAccessToken = tokenStorage.getAccessToken()

  if (
    currentRefreshToken &&
    currentRefreshToken !== refreshToken &&
    currentAccessToken
  ) {
    markSessionActive()
    scheduleAccessTokenRefresh()
    return currentAccessToken
  }

  return null
}

function handleScheduledRefreshFailure(error: unknown): void {
  if (isUnauthorizedApiError(error)) {
    expireAuthenticationSession()
    return
  }

  if (!tokenStorage.getRefreshToken()) return

  // 네트워크·서버 장애를 토큰 만료로 오인하지 않고 잠시 뒤 재시도한다.
  if (refreshTimer) globalThis.clearTimeout(refreshTimer)
  scheduledExpiresAt = null
  refreshTimer = globalThis.setTimeout(() => {
    void refreshAccessToken().catch(handleScheduledRefreshFailure)
  }, refreshRetryDelayMs)
}

async function requestNewAccessToken(refreshToken: string): Promise<string> {
  // 인증 훅이 걸린 apiClient 대신 순수 ky 인스턴스를 써서 재귀 호출을 막는다.
  const body = refreshTokenRequestSchema.parse({ refreshToken })
  let json: unknown

  try {
    const response = await ky.post(`${API_BASE_URL}/auth/refresh`, {
      json: body,
      retry: 0,
    })
    json = await response.json()
  } catch (error) {
    // 다른 요청·탭이 먼저 토큰을 회전했다면 이전 refresh token의 실패로 새 세션을 만료시키지 않는다.
    const advancedAccessToken = getAdvancedSessionAccessToken(refreshToken)
    if (advancedAccessToken) return advancedAccessToken
    throw error
  }

  const { data } = tokenResponseSchema.parse(json)

  // 로그아웃·재로그인 또는 다른 탭의 갱신 뒤 도착한 응답은 현재 세션을 덮어쓰지 않는다.
  if (tokenStorage.getRefreshToken() !== refreshToken) {
    const advancedAccessToken = getAdvancedSessionAccessToken(refreshToken)
    if (advancedAccessToken) return advancedAccessToken
    throw new Error('인증 세션이 변경되었습니다.')
  }

  tokenStorage.setTokens(data.accessToken, data.refreshToken)
  markSessionActive()
  scheduleAccessTokenRefresh()
  return data.accessToken
}

export function scheduleAccessTokenRefresh(): void {
  const accessToken = tokenStorage.getAccessToken()
  if (!accessToken || !tokenStorage.getRefreshToken()) {
    if (refreshTimer) globalThis.clearTimeout(refreshTimer)
    refreshTimer = null
    scheduledExpiresAt = null
    return
  }

  const expiresAt = getAccessTokenExpiresAt(accessToken)
  if (expiresAt === null) {
    if (refreshTimer) globalThis.clearTimeout(refreshTimer)
    refreshTimer = null
    scheduledExpiresAt = null
    return
  }

  // 이미 동일한 만료 시각으로 스케줄링된 타이머가 유효하게 대기 중이면 무분별한 리셋을 무시한다.
  if (refreshTimer !== null && scheduledExpiresAt === expiresAt) {
    return
  }

  if (refreshTimer) globalThis.clearTimeout(refreshTimer)
  scheduledExpiresAt = expiresAt

  const delay = Math.max(0, expiresAt - Date.now() - refreshLeewayMs)
  refreshTimer = globalThis.setTimeout(() => {
    scheduledExpiresAt = null
    refreshTimer = null
    void refreshAccessToken().catch(handleScheduledRefreshFailure)
  }, delay)
}

export function startAccessTokenRefreshScheduler(): void {
  scheduleAccessTokenRefresh()
  if (schedulerStarted || typeof window === 'undefined') return

  schedulerStarted = true

  const handleFocusOrVisibility = () => {
    if (
      typeof document !== 'undefined' &&
      document.visibilityState === 'visible'
    ) {
      scheduleAccessTokenRefresh()
    }
  }

  if (typeof document !== 'undefined') {
    document.addEventListener('visibilitychange', handleFocusOrVisibility)
  }
  window.addEventListener('focus', handleFocusOrVisibility)

  if (!checkInterval) {
    checkInterval = globalThis.setInterval(() => {
      scheduleAccessTokenRefresh()
    }, periodicCheckIntervalMs)
  }
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
