import type { NavigationGuard } from 'vue-router'

import { getMockTransferDetail } from '@/mocks/transfer.mock'
import { resolveTransferStatusRoute } from '@/pages/ward/transfer/-utils/transfer-status-route'
import { useTransferStore } from '@/stores/transfer.store'
import type { TransferStatus } from '@/types/transfer'

const transferStart = { name: 'ward-transfer' }

// TODO(transfer-detail-api): sessionStorage에 없는 거래는 상세 조회 API가
// 제공될 때 실제 응답으로 복구하고, 그전까지 시연용 mock을 유지한다.

export const isValidTransferAccountNumber = (accountNumber: string) =>
  /^\d{8,16}$/.test(accountNumber)

export const requireTransferAccount: NavigationGuard = () => {
  const store = useTransferStore()
  return isValidTransferAccountNumber(store.accountNumber)
    ? true
    : { name: 'ward-transfer-account' }
}

export const requireTransferDraft: NavigationGuard = () => {
  const store = useTransferStore()
  return store.recipient && store.canTransfer ? true : transferStart
}

export const requireTransferRecipient: NavigationGuard = () => {
  const store = useTransferStore()
  return store.recipient ? true : transferStart
}

export const requireTransferIntent: NavigationGuard = () => {
  const store = useTransferStore()
  return store.transferIntent ? true : transferStart
}

export const requireProcessingTransfer: NavigationGuard = () => {
  const store = useTransferStore()
  return store.transferIntent && store.requestStarted ? true : transferStart
}

function getTransactionId(value: unknown) {
  const transactionId = Number(value)
  return Number.isSafeInteger(transactionId) && transactionId > 0
    ? transactionId
    : null
}

function requireTransferStatus(...allowedStatuses: TransferStatus[]) {
  const guard: NavigationGuard = async (to) => {
    const transactionId = getTransactionId(to.params.transactionId)
    if (!transactionId) return transferStart

    const store = useTransferStore()
    const currentDetail =
      store.transferDetail?.transactionId === transactionId
        ? store.transferDetail
        : store.restoreTransferDetail(transactionId)

    try {
      const detail =
        currentDetail ?? (await getMockTransferDetail(transactionId))
      if (!currentDetail) store.setTransferDetail(detail, 'mock')
      if (allowedStatuses.includes(detail.status)) return true

      return (
        resolveTransferStatusRoute(detail.status, transactionId, to.name) ??
        transferStart
      )
    } catch {
      return transferStart
    }
  }
  return guard
}

export const requireCompletedTransfer = requireTransferStatus('COMPLETED')
export const requireHeldTransfer = requireTransferStatus('HELD')
export const requireRejectedTransfer = requireTransferStatus('REJECTED')
export const requirePendingTransfer = requireTransferStatus('HELD')
export const requireCanceledTransfer = requireTransferStatus('CANCELED')
export const requireExpiredTransfer = requireTransferStatus('EXPIRED')
export const requireFailedTransfer = requireTransferStatus('FAILED')
