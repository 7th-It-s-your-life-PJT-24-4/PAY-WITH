import type { NavigationGuard } from 'vue-router'

import { useChargeStore } from '@/stores/charge.store'

const chargeStart = { name: 'ward-charge' }

export const requireRegisteredChargeAccount: NavigationGuard = async () => {
  const store = useChargeStore()
  if (!store.accountsLoaded) await store.loadAccounts()
  return store.accounts.length > 0 ? true : { name: 'ward-charge-account-add' }
}

export const requireNewChargeAccount: NavigationGuard = () => {
  const store = useChargeStore()
  return store.registeredAccount ? true : chargeStart
}

export const requireCompletedCharge: NavigationGuard = async (to) => {
  const transactionId = Number(to.params.transactionId)
  if (!Number.isSafeInteger(transactionId) || transactionId <= 0)
    return chargeStart

  const store = useChargeStore()
  if (store.result?.transactionId === transactionId) return true
  return (await store.restoreResult(transactionId)) ? true : chargeStart
}
