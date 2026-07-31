import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { mockGuardChargeAccounts } from '@/mocks/guard-charge.mock'
import { mockGuardSeniors } from '@/mocks/guard-home.mock'

export const useGuardStore = defineStore('guard', () => {
  const activeSeniorId = ref(mockGuardSeniors[0]?.id ?? '')
  const chargeAmount = ref(0)
  const selectedChargeAccountId = ref(mockGuardChargeAccounts[0]?.id ?? '')

  const activeSenior = computed(
    () =>
      mockGuardSeniors.find(({ id }) => id === activeSeniorId.value) ??
      mockGuardSeniors[0] ??
      null,
  )
  const chargeAccounts = computed(() => mockGuardChargeAccounts)
  const selectedChargeAccount = computed(
    () =>
      chargeAccounts.value.find(
        ({ id }) => id === selectedChargeAccountId.value,
      ) ??
      chargeAccounts.value[0] ??
      null,
  )

  function selectSenior(seniorId: string) {
    if (!mockGuardSeniors.some(({ id }) => id === seniorId)) return
    activeSeniorId.value = seniorId
  }

  function setChargeAmount(value: number) {
    chargeAmount.value = Math.max(0, Math.floor(value))
  }

  function selectChargeAccount(accountId: string) {
    if (!chargeAccounts.value.some(({ id }) => id === accountId)) return
    selectedChargeAccountId.value = accountId
  }

  return {
    activeSeniorId,
    activeSenior,
    chargeAmount,
    chargeAccounts,
    selectedChargeAccount,
    selectedChargeAccountId,
    selectChargeAccount,
    selectSenior,
    setChargeAmount,
  }
})
