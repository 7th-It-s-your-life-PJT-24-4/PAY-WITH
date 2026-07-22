import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

export interface TransferRecipient {
  id: number
  name: string
  relation?: string
  bank: string
  accountNumber: string
}

const initialBalance = 1_250_000

export const useTransferStore = defineStore('transfer', () => {
  const recipient = ref<TransferRecipient | null>(null)
  const accountNumber = ref('')
  const bank = ref('')
  const amount = ref(0)
  const memo = ref('')
  const balance = ref(initialBalance)

  const remainingBalance = computed(() => balance.value - amount.value)
  const canTransfer = computed(
    () =>
      recipient.value !== null &&
      amount.value > 0 &&
      remainingBalance.value >= 0,
  )

  function selectRecipient(value: TransferRecipient) {
    recipient.value = value
    bank.value = value.bank
    accountNumber.value = value.accountNumber
  }

  function selectManualRecipient(selectedBank: string) {
    bank.value = selectedBank
    recipient.value = {
      id: 0,
      name: '김준호',
      bank: selectedBank,
      accountNumber: accountNumber.value,
    }
  }

  function appendAccountDigit(value: string) {
    if (accountNumber.value.length < 16) accountNumber.value += value
  }

  function removeAccountDigit() {
    accountNumber.value = accountNumber.value.slice(0, -1)
  }

  function appendAmountDigit(value: string) {
    const next = Number(`${amount.value || ''}${value}`)
    amount.value = Math.min(next, balance.value)
  }

  function addAmount(value: number) {
    amount.value = Math.min(amount.value + value, balance.value)
  }

  function removeAmountDigit() {
    amount.value = Math.floor(amount.value / 10)
  }

  function reset() {
    recipient.value = null
    accountNumber.value = ''
    bank.value = ''
    amount.value = 0
    memo.value = ''
    balance.value = initialBalance
  }

  return {
    recipient,
    accountNumber,
    bank,
    amount,
    memo,
    balance,
    remainingBalance,
    canTransfer,
    selectRecipient,
    selectManualRecipient,
    appendAccountDigit,
    removeAccountDigit,
    appendAmountDigit,
    addAmount,
    removeAmountDigit,
    reset,
  }
})
