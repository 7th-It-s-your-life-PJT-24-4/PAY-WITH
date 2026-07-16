import ky, { HTTPError } from 'ky'
import { refreshAccessToken } from '@/api/token-refresh'
import { tokenStorage } from '@/api/token-storage'
import type { ZodType as ZodSchema } from 'zod'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

const httpClient = ky.create({
  retry: {
    limit: 1,
    statusCodes: [401],
    methods: ['get', 'post', 'put', 'patch', 'delete'],
  },
  hooks: {
    beforeRequest: [
      ({ request }) => {
        const accessToken = tokenStorage.getAccessToken()
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

        const accessToken = await refreshAccessToken()
        request.headers.set('Authorization', `Bearer ${accessToken}`)
      },
    ],
  },
})

type RequestMethod = 'get' | 'post' | 'put' | 'delete'

async function request<TResponse>(
  path: string,
  method: RequestMethod,
  schema: ZodSchema<TResponse>,
  body?: unknown,
) {
  const response = await httpClient(`${API_BASE_URL}${path}`, {
    method,
    json: body,
    retry: path.startsWith('/auth/') ? 0 : undefined,
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
  ) => request(path, 'post', schema, body),
  put: <TResponse>(path: string, schema: ZodSchema<TResponse>, body: unknown) =>
    request(path, 'put', schema, body),
  delete: <TResponse>(path: string, schema: ZodSchema<TResponse>) =>
    request(path, 'delete', schema),
}
