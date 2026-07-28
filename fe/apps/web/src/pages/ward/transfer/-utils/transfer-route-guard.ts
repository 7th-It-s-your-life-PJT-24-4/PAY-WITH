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

export const redirectPendingTransfer: NavigationGuard = () => {
  const store = useTransferStore()
  // TODO(transfer-pending-lookup): 새로고침 후 /ward/transfer로 직접 진입하면
  // transactionId를 복구할 수 없다. 대기 거래 조회 API 또는 pendingTransferId
  // 제공 정책이 확정되면 서버 상태를 조회해 제한 화면으로 연결한다.
  return store.transferDetail?.status === 'HELD'
    ? {
        name: 'ward-transfer-restricted',
        params: { transactionId: store.transferDetail.transactionId },
      }
    : true
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
