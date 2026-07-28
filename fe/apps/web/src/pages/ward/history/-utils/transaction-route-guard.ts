import type { NavigationGuard } from 'vue-router'

import { getMockWardTransaction } from '@/mocks/transaction.mock'

export const requireWardTransaction: NavigationGuard = (to) => {
  const transactionId = Number(to.params.transactionId)

  if (
    !Number.isSafeInteger(transactionId) ||
    transactionId <= 0 ||
    !getMockWardTransaction(transactionId)
  ) {
    return { name: 'ward-transaction-history', replace: true }
  }

  return true
}
