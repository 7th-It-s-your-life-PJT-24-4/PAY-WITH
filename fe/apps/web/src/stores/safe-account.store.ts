import { ref } from 'vue'
import { defineStore } from 'pinia'

const initialBankName = '국민은행'
const initialAccountNumber = '933'
const recipientName = '안유진'

export const useSafeAccountStore = defineStore('safe-account', () => {
  const bankName = ref(initialBankName)
  const accountNumber = ref(initialAccountNumber)

  function selectBank(value: string) {
    bankName.value = value
  }

  function setAccountNumber(value: string) {
    accountNumber.value = value.replace(/\D/g, '')
  }

  function reset() {
    bankName.value = initialBankName
    accountNumber.value = initialAccountNumber
  }

  return {
    accountNumber,
    bankName,
    recipientName,
    reset,
    selectBank,
    setAccountNumber,
  }
})
