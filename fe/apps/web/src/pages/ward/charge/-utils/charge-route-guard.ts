import type { NavigationGuard } from 'vue-router'

import { useChargeStore } from '@/stores/charge.store'

const chargeStart = { name: 'ward-charge' }

export const requireNewChargeAccount: NavigationGuard = () => {
  const store = useChargeStore()
  return store.registeredAccount ? true : chargeStart
}

export const requireCompletedCharge: NavigationGuard = (to) => {
  const transactionId = Number(to.params.transactionId)
  if (!Number.isSafeInteger(transactionId) || transactionId <= 0)
    return chargeStart

  const store = useChargeStore()
  return store.restoreResult(transactionId) ? true : chargeStart
}
