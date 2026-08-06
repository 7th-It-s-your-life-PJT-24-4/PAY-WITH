import type { NavigationGuard } from 'vue-router'

export const requireWardTransaction: NavigationGuard = (to) => {
  const transactionId = Number(to.params.transactionId)

  if (!Number.isSafeInteger(transactionId) || transactionId <= 0) {
    return { name: 'ward-transaction-history', replace: true }
  }

  return true
}
