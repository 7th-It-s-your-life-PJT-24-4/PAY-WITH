import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  confirmMockTransferStatus,
  getMockBankCode,
  submitMockTransfer,
} from '@/mocks/transfer.mock'

export interface TransferRecipient {
  id: number
  name: string
  relation?: string
  bank: string
  accountNumber: string
}

export interface TransferIntent {
  idempotencyKey: string
  bankCode: string
  bankName: string
  accountNumber: string
  amount: number
  memo: string | null
}

export interface MockTransferResult {
  transactionId: number
  status: 'COMPLETED' | 'HELD'
  idempotencyKey: string
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
  const transferIntent = ref<TransferIntent | null>(null)
  const transferResult = ref<MockTransferResult | null>(null)
  const requestStarted = ref(false)

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

  function createTransferIntent(idempotencyKey = crypto.randomUUID()) {
    if (!recipient.value || !canTransfer.value) return null

    const nextIntent: TransferIntent = {
      idempotencyKey,
      bankCode: getMockBankCode(bank.value),
      bankName: bank.value,
      accountNumber: accountNumber.value.replaceAll('-', ''),
      amount: amount.value,
      memo: memo.value.trim() || null,
    }
    const currentIntent = transferIntent.value
    if (
      currentIntent &&
      !transferResult.value &&
      currentIntent.bankCode === nextIntent.bankCode &&
      currentIntent.accountNumber === nextIntent.accountNumber &&
      currentIntent.amount === nextIntent.amount &&
      currentIntent.memo === nextIntent.memo
    )
      return currentIntent

    transferIntent.value = nextIntent
    transferResult.value = null
    requestStarted.value = false
    processingStatus.value = 'idle'
    processingError.value = ''
    return transferIntent.value
  }

  async function beginMockTransfer(pin: string) {
    if (!transferIntent.value || requestStarted.value) return
    requestStarted.value = true
    processingStatus.value = 'pending'
    processingError.value = ''
    try {
      const result = await submitMockTransfer(
        pin,
        transferIntent.value.idempotencyKey,
      )
      if (result.status === 'unknown') {
        processingStatus.value = 'unknown'
        return
      }
      transferResult.value = result
      processingStatus.value = 'success'
    } catch (error) {
      processingStatus.value = 'error'
      processingError.value =
        error instanceof Error ? error.message : '송금을 완료하지 못했습니다.'
    }
  }

  async function confirmMockStatus() {
    if (!transferIntent.value) return
    processingStatus.value = 'pending'
    transferResult.value = await confirmMockTransferStatus(
      transferIntent.value.idempotencyKey,
    )
    processingStatus.value = 'success'
  }

  function restartAfterFailure(idempotencyKey = crypto.randomUUID()) {
    transferIntent.value = null
    createTransferIntent(idempotencyKey)
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
    transferIntent.value = null
    transferResult.value = null
    requestStarted.value = false
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
    transferIntent,
    transferResult,
    requestStarted,
    remainingBalance,
    canTransfer,
    selectRecipient,
    selectManualRecipient,
    setBankCandidates,
    setVerifiedRecipient,
    createTransferIntent,
    beginMockTransfer,
    confirmMockStatus,
    restartAfterFailure,
    appendAccountDigit,
    removeAccountDigit,
    appendAmountDigit,
    addAmount,
    removeAmountDigit,
    reset,
  }
})
