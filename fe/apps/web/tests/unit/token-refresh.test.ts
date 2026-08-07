import { beforeEach, describe, expect, it, vi } from 'vitest'

import { refreshAccessToken } from '@/api/token-refresh'
import { tokenStorage } from '@/api/token-storage'

const { post } = vi.hoisted(() => ({ post: vi.fn() }))

vi.mock('ky', () => ({ default: { post } }))

function createToken(expiresAt: number) {
  return `header.${btoa(JSON.stringify({ sub: '1', exp: expiresAt }))}.signature`
}

type RefreshResponse = {
  success: boolean
  data: {
    accessToken: string
    refreshToken: string
    tokenType: string
  }
  message: null
}

function createDeferred<T>() {
  let resolve!: (value: T | PromiseLike<T>) => void
  let reject!: (reason?: unknown) => void
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise
    reject = rejectPromise
  })

  return { promise, reject, resolve }
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

  it('does not restore a session cleared while refresh is in flight', async () => {
    tokenStorage.setTokens('expired-access-token', 'refresh-token')
    const refreshResponse = createDeferred<RefreshResponse>()
    post.mockReturnValue({
      json: vi.fn().mockReturnValue(refreshResponse.promise),
    })

    const refresh = refreshAccessToken()
    tokenStorage.clearTokens()
    refreshResponse.resolve({
      success: true,
      data: {
        accessToken: createToken(Math.floor(Date.now() / 1_000) + 900),
        refreshToken: 'rotated-refresh-token',
        tokenType: 'Bearer',
      },
      message: null,
    })

    await expect(refresh).rejects.toThrow('인증 세션이 변경되었습니다.')
    expect(tokenStorage.getAccessToken()).toBeNull()
    expect(tokenStorage.getRefreshToken()).toBeNull()
  })

  it('keeps a newer session when an older refresh fails', async () => {
    tokenStorage.setTokens('expired-access-token', 'refresh-token')
    const refreshResponse = createDeferred<never>()
    post.mockReturnValue({
      json: vi.fn().mockReturnValue(refreshResponse.promise),
    })

    const refresh = refreshAccessToken()
    tokenStorage.setTokens('newer-access-token', 'newer-refresh-token')
    refreshResponse.reject(new Error('old refresh failed'))

    await expect(refresh).resolves.toBe('newer-access-token')
    expect(tokenStorage.getAccessToken()).toBe('newer-access-token')
    expect(tokenStorage.getRefreshToken()).toBe('newer-refresh-token')
  })

  it('does not overwrite a newer session with an older successful response', async () => {
    tokenStorage.setTokens('expired-access-token', 'refresh-token')
    const refreshResponse = createDeferred<RefreshResponse>()
    post.mockReturnValue({
      json: vi.fn().mockReturnValue(refreshResponse.promise),
    })

    const refresh = refreshAccessToken()
    tokenStorage.setTokens('newer-access-token', 'newer-refresh-token')
    refreshResponse.resolve({
      success: true,
      data: {
        accessToken: createToken(Math.floor(Date.now() / 1_000) + 900),
        refreshToken: 'older-rotated-refresh-token',
        tokenType: 'Bearer',
      },
      message: null,
    })

    await expect(refresh).resolves.toBe('newer-access-token')
    expect(tokenStorage.getAccessToken()).toBe('newer-access-token')
    expect(tokenStorage.getRefreshToken()).toBe('newer-refresh-token')
  })
})
