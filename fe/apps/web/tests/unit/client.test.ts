import { beforeEach, describe, expect, it, vi } from 'vitest'
import { z } from 'zod'

import { apiClient } from '@/api/client'
import { refreshAccessToken } from '@/api/token-refresh'
import { tokenStorage } from '@/api/token-storage'

vi.mock('@/api/token-refresh', () => ({
  refreshAccessToken: vi.fn(),
}))

function createDeferred<T>() {
  let resolve!: (value: T | PromiseLike<T>) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })

  return { promise, reject, resolve }
}

describe('apiClient', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('refreshes the access token and retries a non-auth request after a 401', async () => {
    tokenStorage.setTokens('expired-access-token', 'refresh-token')
    vi.mocked(refreshAccessToken).mockResolvedValue('refreshed-access-token')

    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(
        Response.json({ success: true, data: { id: 1 }, message: null }),
      )
    vi.stubGlobal('fetch', fetchMock)

    const responseSchema = z.object({
      success: z.boolean(),
      data: z.object({ id: z.number() }),
      message: z.string().nullable(),
    })

    await expect(apiClient.get('/users/1', responseSchema)).resolves.toEqual({
      success: true,
      data: { id: 1 },
      message: null,
    })

    expect(refreshAccessToken).toHaveBeenCalledOnce()
    expect(fetchMock).toHaveBeenCalledTimes(2)

    const initialRequest = fetchMock.mock.calls[0]?.[0]
    const retriedRequest = fetchMock.mock.calls[1]?.[0]
    expect(initialRequest).toBeInstanceOf(Request)
    expect(retriedRequest).toBeInstanceOf(Request)
    expect((initialRequest as Request).headers.get('Authorization')).toBe(
      'Bearer expired-access-token',
    )
    expect((retriedRequest as Request).headers.get('Authorization')).toBe(
      'Bearer refreshed-access-token',
    )
  })

  it('proactively refreshes the access token before request if token is expiring', async () => {
    const expiringToken =
      'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOjEsImV4cCI6MTAwMDAwMH0.signature'
    tokenStorage.setTokens(expiringToken, 'refresh-token')
    vi.mocked(refreshAccessToken).mockResolvedValue(
      'proactively-refreshed-token',
    )

    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(
        Response.json({ success: true, data: { id: 1 }, message: null }),
      )
    vi.stubGlobal('fetch', fetchMock)

    const responseSchema = z.object({
      success: z.boolean(),
      data: z.object({ id: z.number() }),
      message: z.string().nullable(),
    })

    await expect(apiClient.get('/users/1', responseSchema)).resolves.toEqual({
      success: true,
      data: { id: 1 },
      message: null,
    })

    expect(refreshAccessToken).toHaveBeenCalledOnce()
    expect(fetchMock).toHaveBeenCalledTimes(1)

    const initialRequest = fetchMock.mock.calls[0]?.[0]
    expect(initialRequest).toBeInstanceOf(Request)
    expect((initialRequest as Request).headers.get('Authorization')).toBe(
      'Bearer proactively-refreshed-token',
    )
  })

  it('uses a token refreshed by another request without refreshing again', async () => {
    tokenStorage.setTokens('expired-access-token', 'refresh-token')
    vi.mocked(refreshAccessToken).mockImplementation(async () => {
      tokenStorage.setTokens('refreshed-access-token', 'rotated-refresh-token')
      return 'refreshed-access-token'
    })

    const secondUnauthorized = createDeferred<Response>()
    const fetchMock = vi
      .fn<typeof fetch>()
      .mockImplementation(async (input) => {
        const request = input as Request
        const authorization = request.headers.get('Authorization')

        if (authorization === 'Bearer expired-access-token') {
          if (request.url.endsWith('/users/2'))
            return secondUnauthorized.promise
          return new Response(null, { status: 401 })
        }

        const id = request.url.endsWith('/users/2') ? 2 : 1
        return Response.json({ success: true, data: { id }, message: null })
      })
    vi.stubGlobal('fetch', fetchMock)

    const responseSchema = z.object({
      success: z.boolean(),
      data: z.object({ id: z.number() }),
      message: z.string().nullable(),
    })

    const firstRequest = apiClient.get('/users/1', responseSchema)
    const secondRequest = apiClient.get('/users/2', responseSchema)

    await expect(firstRequest).resolves.toMatchObject({ data: { id: 1 } })
    secondUnauthorized.resolve(new Response(null, { status: 401 }))
    await expect(secondRequest).resolves.toMatchObject({ data: { id: 2 } })

    expect(refreshAccessToken).toHaveBeenCalledOnce()
    expect(fetchMock).toHaveBeenCalledTimes(4)
  })

  it('does not refresh or attach an access token to auth requests', async () => {
    const expiringToken =
      'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOjEsImV4cCI6MTAwMDAwMH0.signature'
    tokenStorage.setTokens(expiringToken, 'refresh-token')
    vi.mocked(refreshAccessToken).mockResolvedValue('refreshed-access-token')

    const fetchMock = vi.fn<typeof fetch>().mockResolvedValueOnce(
      Response.json({
        success: true,
        data: { accessToken: 'access', refreshToken: 'refresh' },
        message: null,
      }),
    )
    vi.stubGlobal('fetch', fetchMock)

    const responseSchema = z.object({
      success: z.boolean(),
      data: z.object({
        accessToken: z.string(),
        refreshToken: z.string(),
      }),
      message: z.string().nullable(),
    })

    await apiClient.post('/auth/login', responseSchema, {
      phone: '01012345678',
      password: 'password',
    })

    expect(refreshAccessToken).not.toHaveBeenCalled()
    const request = fetchMock.mock.calls[0]?.[0]
    expect(request).toBeInstanceOf(Request)
    expect((request as Request).headers.get('Authorization')).toBeNull()
  })

  it('does not attach a stale token when the session is cleared during proactive refresh', async () => {
    const expiringToken =
      'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOjEsImV4cCI6MTAwMDAwMH0.signature'
    tokenStorage.setTokens(expiringToken, 'refresh-token')
    vi.mocked(refreshAccessToken).mockImplementation(async () => {
      tokenStorage.clearTokens()
      throw new Error('session cleared')
    })

    const fetchMock = vi
      .fn<typeof fetch>()
      .mockResolvedValueOnce(
        Response.json({ success: true, data: { id: 1 }, message: null }),
      )
    vi.stubGlobal('fetch', fetchMock)

    const responseSchema = z.object({
      success: z.boolean(),
      data: z.object({ id: z.number() }),
      message: z.string().nullable(),
    })

    await apiClient.get('/users/1', responseSchema)

    const request = fetchMock.mock.calls[0]?.[0]
    expect(request).toBeInstanceOf(Request)
    expect((request as Request).headers.get('Authorization')).toBeNull()
  })
})
