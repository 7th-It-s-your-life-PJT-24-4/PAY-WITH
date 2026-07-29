import type { NavigationGuard } from 'vue-router'

import { getMockPendingTransaction } from '@/mocks/pending-transaction.mock'
import { usePaymentStore } from '@/stores/payment.store'

export const requirePaymentQrSession: NavigationGuard = (to) => {
  const paymentId = Number(to.params.paymentId)
  if (!Number.isSafeInteger(paymentId) || paymentId <= 0)
    return { name: 'ward-payment', replace: true }

  const paymentStore = usePaymentStore()
  paymentStore.restore()
  return paymentStore.qrSession?.paymentId === paymentId
    ? true
    : { name: 'ward-payment', replace: true }
}

export const requireCompletedPayment: NavigationGuard = (to) => {
  const transactionId = Number(to.params.transactionId)
  if (!Number.isSafeInteger(transactionId) || transactionId <= 0)
    return { name: 'ward-transaction-history', replace: true }

  const paymentStore = usePaymentStore()
  paymentStore.restore()
  return paymentStore.completedPayment?.transactionId === transactionId
    ? true
    : { name: 'ward-transaction-history', replace: true }
}

export const requireHeldPayment: NavigationGuard = (to) => {
  const transactionId = Number(to.params.transactionId)
  if (!Number.isSafeInteger(transactionId) || transactionId <= 0)
    return { name: 'ward-home' }

  const transaction = getMockPendingTransaction(transactionId)
  return transaction?.type === 'PAYMENT'
    ? true
    : { name: 'ward-home', replace: true }
}
