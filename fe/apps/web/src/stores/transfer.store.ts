import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  confirmMockTransferStatus,
  submitMockTransfer,
} from '@/mocks/transfer.mock'

export interface TransferRecipient {
  id: number
  name: string
  relation?: string
  bank: string
  accountNumber: string
}

const initialBalance = 1_250_000
export type TransferProcessingStatus =
  'idle' | 'pending' | 'unknown' | 'success' | 'error'

export const useTransferStore = defineStore('transfer', () => {
  const recipient = ref<TransferRecipient | null>(null)
  const accountNumber = ref('')
  const bank = ref('')
  const amount = ref(0)
  const memo = ref('')
  const balance = ref(initialBalance)
  const bankCandidates = ref<string[]>([])
  const processingStatus = ref<TransferProcessingStatus>('idle')
  const processingError = ref('')

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

  function setBankCandidates(value: string[]) {
    bankCandidates.value = value
  }

  function setVerifiedRecipient(name: string, selectedBank: string) {
    selectManualRecipient(selectedBank)
    if (recipient.value) recipient.value.name = name
  }

  async function beginMockTransfer(pin: string) {
    processingStatus.value = 'pending'
    processingError.value = ''
    try {
      processingStatus.value = await submitMockTransfer(pin)
    } catch (error) {
      processingStatus.value = 'error'
      processingError.value =
        error instanceof Error ? error.message : '송금을 완료하지 못했어요.'
    }
  }

  async function confirmMockStatus() {
    processingStatus.value = 'pending'
    processingStatus.value = await confirmMockTransferStatus()
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
    bankCandidates.value = []
    processingStatus.value = 'idle'
    processingError.value = ''
  }

  return {
    recipient,
    accountNumber,
    bank,
    amount,
    memo,
    balance,
    bankCandidates,
    processingStatus,
    processingError,
    remainingBalance,
    canTransfer,
    selectRecipient,
    selectManualRecipient,
    setBankCandidates,
    setVerifiedRecipient,
    beginMockTransfer,
    confirmMockStatus,
    appendAccountDigit,
    removeAccountDigit,
    appendAmountDigit,
    addAmount,
    removeAmountDigit,
    reset,
  }
})
