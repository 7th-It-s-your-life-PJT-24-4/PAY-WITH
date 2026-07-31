import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  chargeResultSchema,
  type ChargeAccount,
  type ChargeResult,
} from '@/schemas/charge.schema'

const resultStorageKey = 'pay-with:ward-charge-result'

export const useChargeStore = defineStore('charge', () => {
  const selectedAccountId = ref<number | null>(null)
  const amount = ref(0)
  const registeredAccount = ref<ChargeAccount | null>(null)
  const result = ref<ChargeResult | null>(null)

  const canCharge = computed(
    () =>
      selectedAccountId.value !== null &&
      Number.isSafeInteger(amount.value) &&
      amount.value > 0,
  )

  function syncAccounts(accounts: ChargeAccount[]) {
    if (
      selectedAccountId.value === null ||
      !accounts.some(({ accountId }) => accountId === selectedAccountId.value)
    ) {
      selectedAccountId.value = accounts[0]?.accountId ?? null
    }
  }

  function selectAccount(accountId: number) {
    selectedAccountId.value = accountId
  }

  function setAmount(value: number) {
    amount.value = Math.max(0, Math.floor(value))
  }

  function addAmount(value: number) {
    setAmount(amount.value + value)
  }

  function saveRegisteredAccount(account: ChargeAccount) {
    registeredAccount.value = account
    selectedAccountId.value = account.accountId
  }

  function saveResult(value: ChargeResult) {
    result.value = value
    sessionStorage.setItem(resultStorageKey, JSON.stringify(value))
  }

  function restoreResult(transactionId: number) {
    if (result.value?.transactionId === transactionId) return result.value

    try {
      const saved: unknown = JSON.parse(
        sessionStorage.getItem(resultStorageKey) ?? 'null',
      )
      const parsed = chargeResultSchema.safeParse(saved)
      if (!parsed.success || parsed.data.transactionId !== transactionId)
        return null
      result.value = parsed.data
      return result.value
    } catch {
      return null
    }
  }

  function resetDraft() {
    amount.value = 0
    result.value = null
    sessionStorage.removeItem(resultStorageKey)
  }

  return {
    selectedAccountId,
    amount,
    registeredAccount,
    result,
    canCharge,
    syncAccounts,
    selectAccount,
    setAmount,
    addAmount,
    saveRegisteredAccount,
    saveResult,
    restoreResult,
    resetDraft,
  }
})
