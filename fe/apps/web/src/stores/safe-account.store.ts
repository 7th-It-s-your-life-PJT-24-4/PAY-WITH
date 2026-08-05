import { ref } from 'vue'
import { defineStore } from 'pinia'

const initialBankName = ''
const initialBankCode = ''
const initialAccountNumber = ''

export const useSafeAccountStore = defineStore('safe-account', () => {
  const bankName = ref(initialBankName)
  const bankCode = ref(initialBankCode)
  const accountNumber = ref(initialAccountNumber)

  function selectBank(value: { code: string; name: string } | string) {
    if (typeof value === 'string') {
      bankName.value = value
      return
    }

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
    reset,
    selectBank,
    setAccountNumber,
  }
})
