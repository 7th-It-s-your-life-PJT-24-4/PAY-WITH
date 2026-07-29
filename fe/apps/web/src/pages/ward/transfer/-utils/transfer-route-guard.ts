import type { NavigationGuard } from 'vue-router'

import { getMockTransferDetail } from '@/mocks/transfer.mock'
import { resolveTransferStatusRoute } from '@/pages/ward/transfer/-utils/transfer-status-route'
import { useTransferStore } from '@/stores/transfer.store'
import type { TransferStatus } from '@/types/transfer'

const transferStart = { name: 'ward-transfer' }

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

    try {
      const detail = await getMockTransferDetail(transactionId)
      const store = useTransferStore()
      store.setTransferDetail(detail)
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
