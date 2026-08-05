import { beforeEach, describe, expect, it, vi } from 'vitest'

import { refreshAccessToken } from '@/api/token-refresh'
import { tokenStorage } from '@/api/token-storage'

const { post } = vi.hoisted(() => ({ post: vi.fn() }))

vi.mock('ky', () => ({ default: { post } }))

function createToken(expiresAt: number) {
  return `header.${btoa(JSON.stringify({ sub: '1', exp: expiresAt }))}.signature`
}

describe('refreshAccessToken', () => {
  beforeEach(() => {
    localStorage.clear()
    post.mockReset()
  })

  it('shares one refresh request and rotates both tokens', async () => {
    const accessToken = createToken(Math.floor(Date.now() / 1_000) + 900)
    tokenStorage.setTokens('expired-access-token', 'refresh-token')
    post.mockReturnValue({
      json: vi.fn().mockResolvedValue({
        success: true,
        data: {
          accessToken,
          refreshToken: 'rotated-refresh-token',
          tokenType: 'Bearer',
        },
        message: null,
      }),
    })

    const [first, second] = await Promise.all([
      refreshAccessToken(),
      refreshAccessToken(),
    ])

    expect(post).toHaveBeenCalledOnce()
    expect(first).toBe(accessToken)
    expect(second).toBe(accessToken)
    expect(tokenStorage.getAccessToken()).toBe(accessToken)
    expect(tokenStorage.getRefreshToken()).toBe('rotated-refresh-token')
  })

  it('keeps refresh failures observable to the session coordinator', async () => {
    tokenStorage.setTokens('expired-access-token', 'refresh-token')
    post.mockReturnValue({
      json: vi.fn().mockRejectedValue(new Error('refresh failed')),
    })

    await expect(refreshAccessToken()).rejects.toThrow('refresh failed')
  })
})
