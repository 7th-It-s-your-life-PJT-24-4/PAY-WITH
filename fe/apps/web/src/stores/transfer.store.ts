import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getTransferBankCode } from '@/pages/ward/transfer/-utils/transfer-bank'
import { getTransferApiError } from '@/pages/ward/transfer/-utils/transfer-api-error'
import type {
  CreateTransferRequest,
  TransferResult,
} from '@/schemas/transfer.schema'
import type { TransferDetail } from '@/types/transfer'

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
  accountNo: string
  amount: number
  memo: string | null
}

export interface TransferSubmissionResult {
  transactionId: number
  status: 'COMPLETED' | 'HELD'
  idempotencyKey: string
}

type TransferExecutor = (variables: {
  request: CreateTransferRequest
  idempotencyKey: string
}) => Promise<TransferResult>

const initialBalance = 1_250_000
export type TransferProcessingStatus =
  'idle' | 'pending' | 'held' | 'unknown' | 'success' | 'error'

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
  const transferResult = ref<TransferSubmissionResult | null>(null)
  const transferDetail = ref<TransferDetail | null>(null)
  const transferDetailSource = ref<'api' | 'mock' | null>(null)
  const requestStarted = ref(false)
  const pendingPin = ref('')

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
    const bankCode = getTransferBankCode(bank.value)
    if (!bankCode) return null

    const nextIntent: TransferIntent = {
      idempotencyKey,
      bankCode,
      bankName: bank.value,
      accountNo: accountNumber.value.replaceAll('-', ''),
      amount: amount.value,
      memo: memo.value.trim() || null,
    }
    const currentIntent = transferIntent.value
    if (
      currentIntent &&
      !transferResult.value &&
      currentIntent.bankCode === nextIntent.bankCode &&
      currentIntent.accountNo === nextIntent.accountNo &&
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

  function setTransferDetail(
    detail: TransferDetail,
    source: 'api' | 'mock' = 'mock',
  ) {
    transferDetail.value = detail
    transferDetailSource.value = source
  }

  function createTransferDetail(result: TransferResult): TransferDetail {
    const requestedAt = new Date().toISOString()
    const completed = result.status === 'COMPLETED'
    return {
      transactionId: result.transactionId,
      status: result.status,
      holderName: completed ? result.holderName : (recipient.value?.name ?? ''),
      bankCode: completed
        ? result.bankCode
        : (transferIntent.value?.bankCode ?? ''),
      bankName: completed ? result.bankName : bank.value,
      accountNo: completed ? result.accountNo : accountNumber.value,
      amount: completed ? result.amount : amount.value,
      memo: completed ? result.memo : memo.value.trim() || null,
      requestedAt,
      expiredAt: null,
      respondedAt: completed ? requestedAt : null,
      completedAt: completed ? result.completedAt : null,
      balanceAfter: completed ? result.balanceAfter : null,
      riskAnalysis: null,
      failureCode: null,
      failureMessage: null,
    }
  }

  async function executeTransfer(execute: TransferExecutor) {
    if (!transferIntent.value || !pendingPin.value) return
    processingStatus.value = 'pending'
    processingError.value = ''
    const intent = transferIntent.value
    try {
      const result = await execute({
        request: {
          bankCode: intent.bankCode,
          accountNo: intent.accountNo,
          amount: intent.amount,
          memo: intent.memo,
          transferPin: pendingPin.value,
        },
        idempotencyKey: intent.idempotencyKey,
      })
      transferResult.value = {
        transactionId: result.transactionId,
        status: result.status,
        idempotencyKey: intent.idempotencyKey,
      }
      setTransferDetail(createTransferDetail(result), 'api')
      pendingPin.value = ''
      if (result.status === 'HELD') {
        processingStatus.value = 'held'
        return
      }
      balance.value = result.balanceAfter
      processingStatus.value = 'success'
    } catch (error) {
      const apiError = await getTransferApiError(
        error,
        '송금을 완료하지 못했습니다.',
      )
      if (apiError.status === 409 && apiError.message.includes('처리 중')) {
        processingStatus.value = 'unknown'
        return
      }
      pendingPin.value = ''
      requestStarted.value = false
      processingStatus.value = 'error'
      processingError.value = apiError.message
    }
  }

  async function beginTransfer(pin: string, execute: TransferExecutor) {
    if (!transferIntent.value || requestStarted.value) return
    requestStarted.value = true
    pendingPin.value = pin
    await executeTransfer(execute)
  }

  async function confirmTransferStatus(execute: TransferExecutor) {
    if (processingStatus.value !== 'unknown') return
    await executeTransfer(execute)
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
    transferDetail.value = null
    transferDetailSource.value = null
    requestStarted.value = false
    pendingPin.value = ''
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
    transferDetail,
    transferDetailSource,
    requestStarted,
    remainingBalance,
    canTransfer,
    setTransferDetail,
    selectRecipient,
    selectManualRecipient,
    setBankCandidates,
    setVerifiedRecipient,
    createTransferIntent,
    beginTransfer,
    confirmTransferStatus,
    restartAfterFailure,
    appendAccountDigit,
    removeAccountDigit,
    appendAmountDigit,
    addAmount,
    removeAmountDigit,
    reset,
  }
})
