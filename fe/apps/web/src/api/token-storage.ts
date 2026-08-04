const ACCESS_TOKEN_KEY = 'accessToken'
const REFRESH_TOKEN_KEY = 'refreshToken'

type AccessTokenPayload = {
  sub?: unknown
  exp?: unknown
}

function getAccessTokenPayload(accessToken: string): AccessTokenPayload | null {
  try {
    const payload = accessToken.split('.')[1]
    if (!payload) return null

    const normalizedPayload = payload
      .replace(/-/g, '+')
      .replace(/_/g, '/')
      .padEnd(payload.length + ((4 - (payload.length % 4)) % 4), '=')
    return JSON.parse(atob(normalizedPayload)) as AccessTokenPayload
  } catch {
    return null
  }
}

export function getUserIdFromAccessToken(accessToken: string): number | null {
  const payload = getAccessTokenPayload(accessToken)
  const userId = Number(payload?.sub)

  return Number.isSafeInteger(userId) && userId > 0 ? userId : null
}

export function getAccessTokenExpiresAt(accessToken: string): number | null {
  const expiresAt = Number(getAccessTokenPayload(accessToken)?.exp)
  return Number.isFinite(expiresAt) && expiresAt > 0 ? expiresAt * 1_000 : null
}

export function isAccessTokenExpiring(
  accessToken: string,
  leewayMs = 60_000,
): boolean {
  const expiresAt = getAccessTokenExpiresAt(accessToken)
  return expiresAt !== null && expiresAt <= Date.now() + leewayMs
}

// 이 프로젝트는 쿠키 기반 세션을 쓰지 않으므로 토큰을 localStorage에 보관한다.
export const tokenStorage = {
  getAccessToken(): string | null {
    return localStorage.getItem(ACCESS_TOKEN_KEY)
  },
  getRefreshToken(): string | null {
    return localStorage.getItem(REFRESH_TOKEN_KEY)
  },
  setTokens(accessToken: string, refreshToken: string): void {
    localStorage.setItem(ACCESS_TOKEN_KEY, accessToken)
    localStorage.setItem(REFRESH_TOKEN_KEY, refreshToken)
  },
  clearTokens(): void {
    localStorage.removeItem(ACCESS_TOKEN_KEY)
    localStorage.removeItem(REFRESH_TOKEN_KEY)
  },
}
