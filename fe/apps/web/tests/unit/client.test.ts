import { beforeEach, describe, expect, it, vi } from 'vitest'
import { z } from 'zod'

import { apiClient } from '@/api/client'
import { refreshAccessToken } from '@/api/token-refresh'
import { tokenStorage } from '@/api/token-storage'

vi.mock('@/api/token-refresh', () => ({
  refreshAccessToken: vi.fn(),
}))

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
})
