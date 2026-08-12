import type { User } from '@/schemas/user.schema'

export function getRoleHomePath(role: User['role']) {
  return role === 'GUARD' ? '/guard' : '/ward'
}

export function getUnauthenticatedSignInQuery(
  fullPath: string,
  source: unknown,
  sessionExpired: boolean,
): { reason?: string; redirect?: string } | undefined {
  const shouldRestoreDestination = sessionExpired || source === 'push'
  if (!shouldRestoreDestination) return undefined

  return {
    ...(sessionExpired ? { reason: 'session-expired' } : {}),
    redirect: fullPath,
  }
}

export function getSafePostLoginPath(
  value: unknown,
  role: User['role'],
): string | null {
  if (
    typeof value !== 'string' ||
    !value.startsWith('/') ||
    value.startsWith('//')
  )
    return null
  const pathname = value.split(/[?#]/, 1)[0] ?? value
  if (pathname.startsWith('/auth')) return null

  const rolePrefix = role === 'GUARD' ? '/guard' : '/ward'
  if (pathname !== rolePrefix && !pathname.startsWith(`${rolePrefix}/`))
    return null

  const unsafeFlowPrefixes = [
    '/guard/charge',
    '/ward/charge',
    '/ward/payment',
    '/ward/transfer',
  ]
  return unsafeFlowPrefixes.some((prefix) => pathname.startsWith(prefix))
    ? null
    : value
}
