import type { NavigationGuard } from 'vue-router'

import { getMockPendingTransaction } from '@/mocks/pending-transaction.mock'

export const requireHeldPayment: NavigationGuard = (to) => {
  const transactionId = Number(to.params.transactionId)
  if (!Number.isSafeInteger(transactionId) || transactionId <= 0)
    return { name: 'ward-home' }

  const transaction = getMockPendingTransaction(transactionId)
  return transaction?.type === 'PAYMENT'
    ? true
    : { name: 'ward-home', replace: true }
}
