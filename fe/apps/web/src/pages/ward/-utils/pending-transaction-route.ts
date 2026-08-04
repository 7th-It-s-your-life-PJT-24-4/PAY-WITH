import type { RouteLocationRaw } from 'vue-router'

import type { PendingTransaction } from '@/types/pending-transaction'

export function getPendingTransactionRoute(
  transaction: PendingTransaction,
): RouteLocationRaw {
  return {
    name:
      transaction.type === 'TRANSFER'
        ? 'ward-transfer-held'
        : 'ward-payment-held',
    params: { transactionId: transaction.transactionId },
  }
}
