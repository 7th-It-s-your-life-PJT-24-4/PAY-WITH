import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { registerGuardSafeAccountRequestSchema } from '@/schemas/guard-safe-account.schema'

const initialBankName = ''
const initialBankCode = ''
const initialAccountNumber = ''

export const useSafeAccountStore = defineStore('safe-account', () => {
  const bankName = ref(initialBankName)
  const bankCode = ref(initialBankCode)
  const accountNumber = ref(initialAccountNumber)
  const registrationDraft = computed(() => {
    const result = registerGuardSafeAccountRequestSchema.safeParse({
      bankCode: bankCode.value,
      accountNo: accountNumber.value,
    })

    return result.success ? result.data : null
  })

  function selectBank(value: { code: string; name: string }) {
    bankCode.value = value.code
    bankName.value = value.name
  }

  function setAccountNumber(value: string) {
    accountNumber.value = value.replace(/\D/g, '')
  }

  function reset() {
    bankName.value = initialBankName
    bankCode.value = initialBankCode
    accountNumber.value = initialAccountNumber
  }

  return {
    accountNumber,
    bankCode,
    bankName,
    registrationDraft,
    reset,
    selectBank,
    setAccountNumber,
  }
})
