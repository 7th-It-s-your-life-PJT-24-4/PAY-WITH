import type { RouteLocationRaw, RouteRecordNameGeneric } from 'vue-router'

import type { TransferStatus } from '@/types/transfer'

const terminalRouteNames: Partial<Record<TransferStatus, string>> = {
  COMPLETED: 'ward-transfer-complete',
  REJECTED: 'ward-transfer-rejected',
}

export function resolveTransferStatusRoute(
  status: TransferStatus,
  transactionId: number,
  currentRouteName?: RouteRecordNameGeneric,
): RouteLocationRaw | null {
  if (
    status === 'HELD' &&
    (currentRouteName === 'ward-transfer-held' ||
      currentRouteName === 'ward-transfer-restricted')
  )
    return null

  if (status === 'HELD')
    return {
      name: 'ward-transfer-held',
      params: { transactionId },
      replace: true,
    }

  const routeName = terminalRouteNames[status]
  if (routeName && currentRouteName !== routeName)
    return {
      name: routeName,
      params: { transactionId },
      replace: true,
    }

  if (routeName) return null

  return { name: 'ward-home', replace: true }
}

export function formatTransferDateTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value

  const parts = new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).formatToParts(date)
  const getPart = (type: Intl.DateTimeFormatPartTypes) =>
    parts.find((part) => part.type === type)?.value ?? ''

  return `${getPart('year')}.${getPart('month')}.${getPart('day')} ${getPart('hour')}:${getPart('minute')}`
}
