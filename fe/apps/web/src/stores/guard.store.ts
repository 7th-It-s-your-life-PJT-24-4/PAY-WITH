import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import type { GuardChargeResult } from '@/schemas/charge.schema'

export const useGuardStore = defineStore('guard', () => {
  const activeWardId = ref<number | null>(null)
  const chargeAmount = ref(0)
  const selectedChargeAccountId = ref<number | null>(null)
  const lastChargeResult = ref<GuardChargeResult | null>(null)

  const canSubmitCharge = computed(
    () =>
      activeWardId.value !== null &&
      selectedChargeAccountId.value !== null &&
      chargeAmount.value > 0,
  )

  function selectWard(wardId: number) {
    if (!Number.isSafeInteger(wardId) || wardId <= 0) return
    activeWardId.value = wardId
  }

  function clearWardSelection() {
    activeWardId.value = null
  }

  function setChargeAmount(value: number) {
    chargeAmount.value = Math.max(0, Math.floor(value))
  }

  function selectChargeAccount(accountId: number) {
    if (!Number.isSafeInteger(accountId) || accountId <= 0) return
    selectedChargeAccountId.value = accountId
  }

  function saveChargeResult(result: GuardChargeResult) {
    lastChargeResult.value = result
  }

  return {
    activeWardId,
    chargeAmount,
    selectedChargeAccountId,
    lastChargeResult,
    canSubmitCharge,
    clearWardSelection,
    selectChargeAccount,
    selectWard,
    saveChargeResult,
    setChargeAmount,
  }
})
