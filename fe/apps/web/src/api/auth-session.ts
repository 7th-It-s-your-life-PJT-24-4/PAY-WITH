import { tokenStorage } from '@/api/token-storage'

type SessionExpiredHandler = () => void | Promise<void>

let sessionExpiredHandler: SessionExpiredHandler | null = null
let expirationHandled = false

export function configureSessionExpiredHandler(
  handler: SessionExpiredHandler,
): void {
  sessionExpiredHandler = handler
}

export function markSessionActive(): void {
  expirationHandled = false
}

export function clearAuthenticationSession(): void {
  tokenStorage.clearTokens()
  if (typeof sessionStorage !== 'undefined') sessionStorage.clear()
}

export function expireAuthenticationSession(): void {
  clearAuthenticationSession()
  if (expirationHandled) return

  expirationHandled = true
  void sessionExpiredHandler?.()
}
