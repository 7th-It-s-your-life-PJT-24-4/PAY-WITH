import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { mockGuardSeniors } from '@/mocks/guard-home.mock'

export const useGuardStore = defineStore('guard', () => {
  const activeSeniorId = ref(mockGuardSeniors[0]?.id ?? '')
  const chargeAmount = ref(0)

  const activeSenior = computed(
    () =>
      mockGuardSeniors.find(({ id }) => id === activeSeniorId.value) ??
      mockGuardSeniors[0] ??
      null,
  )

  function selectSenior(seniorId: string) {
    if (!mockGuardSeniors.some(({ id }) => id === seniorId)) return
    activeSeniorId.value = seniorId
  }

  function setChargeAmount(value: number) {
    chargeAmount.value = Math.max(0, Math.floor(value))
  }

  return {
    activeSeniorId,
    activeSenior,
    chargeAmount,
    selectSenior,
    setChargeAmount,
  }
})
