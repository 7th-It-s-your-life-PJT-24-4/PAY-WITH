const PUSH_TOKEN_KEY = 'payWithFcmToken'

type StoredPushToken = {
  token: string
  userId: number
}

export const pushTokenStorage = {
  get(): StoredPushToken | null {
    try {
      const value = localStorage.getItem(PUSH_TOKEN_KEY)
      if (!value) return null
      const parsed = JSON.parse(value) as Partial<StoredPushToken>
      if (
        typeof parsed.token !== 'string' ||
        parsed.token.length === 0 ||
        !Number.isSafeInteger(parsed.userId) ||
        Number(parsed.userId) <= 0
      ) {
        return null
      }
      return { token: parsed.token, userId: Number(parsed.userId) }
    } catch {
      return null
    }
  },
  set(value: StoredPushToken): void {
    localStorage.setItem(PUSH_TOKEN_KEY, JSON.stringify(value))
  },
  clear(): void {
    localStorage.removeItem(PUSH_TOKEN_KEY)
  },
}
