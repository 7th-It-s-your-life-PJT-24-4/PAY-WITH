import type { NavigationGuard } from 'vue-router'

import { useTransferStore } from '@/stores/transfer.store'

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

export const requireCompletedTransfer: NavigationGuard = (to) => {
  const store = useTransferStore()
  const result = store.transferResult
  return result &&
    result.status === 'COMPLETED' &&
    String(result.transactionId) === String(to.params.transactionId)
    ? true
    : transferStart
}
