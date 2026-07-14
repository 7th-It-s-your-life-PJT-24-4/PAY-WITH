import type { ZodSchema } from 'zod'

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL

type RequestOptions = Omit<RequestInit, 'body'> & {
  body?: unknown
}

async function request<TResponse>(
  path: string,
  schema: ZodSchema<TResponse>,
  options: RequestOptions = {},
) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
    body: options.body ? JSON.stringify(options.body) : undefined,
  })

  if (!response.ok) {
    throw new Error(`API 요청 실패: ${response.status}`)
  }

  const data: unknown = await response.json()
  return schema.parse(data)
}

export const apiClient = {
  get: <TResponse>(path: string, schema: ZodSchema<TResponse>) =>
    request(path, schema),
  post: <TResponse>(
    path: string,
    schema: ZodSchema<TResponse>,
    body: unknown,
  ) => request(path, schema, { method: 'POST', body }),
}
