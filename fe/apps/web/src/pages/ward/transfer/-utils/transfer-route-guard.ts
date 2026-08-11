import type { NavigationGuard } from 'vue-router'

import { resolveTransferStatusRoute } from '@/pages/ward/transfer/-utils/transfer-status-route'
import { useTransferStore } from '@/stores/transfer.store'
import type { TransferStatus } from '@/types/transfer'

export const isValidTransferAccountNumber = (accountNumber: string) =>
  /^\d{8,16}$/.test(accountNumber)

const transferStart = { name: 'ward-transfer' }

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

    if (!currentDetail) return transferStart
    if (allowedStatuses.includes(currentDetail.status)) return true

    return (
      resolveTransferStatusRoute(
        currentDetail.status,
        transactionId,
        to.name,
      ) ?? transferStart
    )
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
