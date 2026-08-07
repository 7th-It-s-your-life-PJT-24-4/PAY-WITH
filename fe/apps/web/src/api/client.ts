import ky, { HTTPError } from 'ky'
import { expireAuthenticationSession } from '@/api/auth-session'
import { isUnauthorizedApiError } from '@/api/error'
import { refreshAccessToken } from '@/api/token-refresh'
import { isAccessTokenExpiring, tokenStorage } from '@/api/token-storage'
import type { ZodType as ZodSchema } from 'zod'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

const authenticatedHttpClient = ky.create({
  retry: {
    limit: 1,
    statusCodes: [401],
    methods: ['get', 'post', 'put', 'patch', 'delete'],
  },
  hooks: {
    beforeRequest: [
      async ({ request }) => {
        let accessToken = tokenStorage.getAccessToken()
        const refreshToken = tokenStorage.getRefreshToken()

        if (accessToken && refreshToken && isAccessTokenExpiring(accessToken)) {
          try {
            accessToken = await refreshAccessToken()
          } catch {
            // 사전 갱신 실패 시 기존 토큰으로 시도하고 401 수신 시 beforeRetry에 처리를 위임한다.
            // 대기 중 로그아웃·재로그인이 발생했다면 바뀐 세션 상태를 따른다.
            accessToken = tokenStorage.getAccessToken()
          }
        }

        if (accessToken) {
          request.headers.set('Authorization', `Bearer ${accessToken}`)
        }
      },
    ],
    beforeRetry: [
      async ({ request, error }) => {
        if (!(error instanceof HTTPError) || error.response.status !== 401) {
          return
        }

        const currentAccessToken = tokenStorage.getAccessToken()
        if (
          currentAccessToken &&
          request.headers.get('Authorization') !==
            `Bearer ${currentAccessToken}`
        ) {
          // 다른 요청이 이미 토큰을 갱신했다면 회전된 refresh token을 다시 쓰지 않는다.
          request.headers.set('Authorization', `Bearer ${currentAccessToken}`)
          return
        }

        try {
          const accessToken = await refreshAccessToken()
          request.headers.set('Authorization', `Bearer ${accessToken}`)
        } catch (refreshError) {
          if (
            isUnauthorizedApiError(refreshError) ||
            !tokenStorage.getRefreshToken()
          ) {
            expireAuthenticationSession()
          }
          throw refreshError
        }
      },
    ],
  },
})

const unauthenticatedHttpClient = ky.create({ retry: 0 })

type RequestMethod = 'get' | 'post' | 'put' | 'delete'

async function request<TResponse>(
  path: string,
  method: RequestMethod,
  schema: ZodSchema<TResponse>,
  body?: unknown,
  headers?: HeadersInit,
) {
  const httpClient = path.startsWith('/auth/')
    ? unauthenticatedHttpClient
    : authenticatedHttpClient
  const response = await httpClient(`${API_BASE_URL}${path}`, {
    method,
    json: body,
    headers,
  })

  const data: unknown = await response.json()
  return schema.parse(data)
}

export const apiClient = {
  get: <TResponse>(path: string, schema: ZodSchema<TResponse>) =>
    request(path, 'get', schema),
  post: <TResponse>(
    path: string,
    schema: ZodSchema<TResponse>,
    body: unknown,
    headers?: HeadersInit,
  ) => request(path, 'post', schema, body, headers),
  put: <TResponse>(path: string, schema: ZodSchema<TResponse>, body: unknown) =>
    request(path, 'put', schema, body),
  delete: <TResponse>(path: string, schema: ZodSchema<TResponse>) =>
    request(path, 'delete', schema),
}
