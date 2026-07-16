import ky from 'ky'

import { tokenStorage } from '@/api/token-storage'
import {
  refreshTokenRequestSchema,
  tokenResponseSchema,
} from '@/schemas/auth.schema'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

let refreshPromise: Promise<string> | null = null

async function requestNewAccessToken(refreshToken: string): Promise<string> {
  // 인증 훅이 걸린 apiClient 대신 순수 ky 인스턴스를 써서 재귀 호출을 막는다.
  const body = refreshTokenRequestSchema.parse({ refreshToken })
  const response = await ky.post(`${API_BASE_URL}/auth/refresh`, { json: body })
  const json: unknown = await response.json()
  const { data } = tokenResponseSchema.parse(json)

  tokenStorage.setTokens(data.accessToken, data.refreshToken)
  return data.accessToken
}

// 동시에 여러 요청이 401을 받아도 refresh 호출은 한 번만 나가도록 in-flight 요청을 공유한다.
export async function refreshAccessToken(): Promise<string> {
  const refreshToken = tokenStorage.getRefreshToken()

  if (!refreshToken) {
    tokenStorage.clearTokens()
    throw new Error('저장된 refresh token이 없습니다.')
  }

  if (!refreshPromise) {
    refreshPromise = requestNewAccessToken(refreshToken).finally(() => {
      refreshPromise = null
    })
  }

  try {
    return await refreshPromise
  } catch (error) {
    tokenStorage.clearTokens()
    throw error
  }
}
