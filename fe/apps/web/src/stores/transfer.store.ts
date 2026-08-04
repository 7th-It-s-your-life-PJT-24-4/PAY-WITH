import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { getTransferBankCode } from '@/pages/ward/transfer/-utils/transfer-bank'
import {
  getTransferApiError,
  getTransferFailureAction,
  type TransferFailureAction,
} from '@/pages/ward/transfer/-utils/transfer-api-error'
import type {
  CreateTransferRequest,
  TransferResult,
} from '@/schemas/transfer.schema'
import { storedTransferDetailSchema } from '@/schemas/transfer.schema'
import type { TransferDetail } from '@/types/transfer'

export interface TransferRecipient {
  id: number
  name: string
  holderName?: string
  bankCode?: string
  relation?: string
  bank: string
  accountNumber: string
  isContact?: boolean
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

// 상세 조회 API가 준비되기 전까지 실제 송금 결과를 거래별로 보관해
// 완료·승인 대기 화면을 새로고침해도 같은 브라우저 세션에서 복구한다.
const transferResultStorageKey = (transactionId: number) =>
  `pay-with:ward-transfer:${transactionId}`
export type TransferProcessingStatus =
  'idle' | 'pending' | 'held' | 'unknown' | 'success' | 'error'

export const useTransferStore = defineStore('transfer', () => {
  const recipient = ref<TransferRecipient | null>(null)
  const accountNumber = ref('')
  const bank = ref('')
  const amount = ref(0)
  const memo = ref('')
  const balance = ref<number | null>(null)
  const bankCandidates = ref<string[]>([])
  const processingStatus = ref<TransferProcessingStatus>('idle')
  const processingError = ref('')
  const processingFailureAction = ref<TransferFailureAction | null>(null)
  const transferIntent = ref<TransferIntent | null>(null)
  const transferResult = ref<TransferSubmissionResult | null>(null)
  const transferDetail = ref<TransferDetail | null>(null)
  const transferDetailSource = ref<'api' | 'mock' | null>(null)
  const requestStarted = ref(false)
  const pendingPin = ref('')

  const remainingBalance = computed(() =>
    balance.value === null ? null : balance.value - amount.value,
  )
  const isAmountOverBalance = computed(
    () => remainingBalance.value !== null && remainingBalance.value < 0,
  )
  const canTransfer = computed(
    () =>
      recipient.value !== null &&
      balance.value !== null &&
      amount.value > 0 &&
      remainingBalance.value !== null &&
      remainingBalance.value >= 0,
  )

  function setBalance(value: number) {
    balance.value = value
  }

  function selectRecipient(value: TransferRecipient) {
    recipient.value = value
    bank.value = value.bank
    accountNumber.value = value.accountNumber
  }

  function selectManualRecipient(selectedBank: string, bankCode?: string) {
    bank.value = selectedBank
    recipient.value = {
      id: 0,
      name: '김준호',
      bankCode,
      bank: selectedBank,
      accountNumber: accountNumber.value,
    }
  }

  function setBankCandidates(value: string[]) {
    bankCandidates.value = value
  }

  function setVerifiedRecipient(
    name: string,
    selectedBank: string,
    bankCode: string,
  ) {
    selectManualRecipient(selectedBank, bankCode)
    if (recipient.value) recipient.value.name = name
  }

  function createTransferIntent(idempotencyKey = crypto.randomUUID()) {
    if (!recipient.value || !canTransfer.value) return null
    const bankCode = recipient.value.bankCode ?? getTransferBankCode(bank.value)
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

    clearStoredTransferDetail()
    transferIntent.value = nextIntent
    transferResult.value = null
    requestStarted.value = false
    processingStatus.value = 'idle'
    processingError.value = ''
    processingFailureAction.value = null
    return transferIntent.value
  }

  function setTransferDetail(
    detail: TransferDetail,
    source: 'api' | 'mock' = 'mock',
  ) {
    transferDetail.value = detail
    transferDetailSource.value = source
    if (source === 'api' && typeof sessionStorage !== 'undefined')
      sessionStorage.setItem(
        transferResultStorageKey(detail.transactionId),
        JSON.stringify(detail),
      )
  }

  function restoreTransferDetail(transactionId: number) {
    if (typeof sessionStorage === 'undefined') return null
    const key = transferResultStorageKey(transactionId)
    try {
      const raw = sessionStorage.getItem(key)
      if (!raw) return null
      const parsed = storedTransferDetailSchema.safeParse(JSON.parse(raw))
      if (!parsed.success || parsed.data.transactionId !== transactionId) {
        sessionStorage.removeItem(key)
        return null
      }
      transferDetail.value = parsed.data
      transferDetailSource.value = 'api'
      return transferDetail.value
    } catch {
      sessionStorage.removeItem(key)
      return null
    }
  }

  function clearStoredTransferDetail(
    transactionId = transferDetail.value?.transactionId,
  ) {
    if (transactionId && typeof sessionStorage !== 'undefined')
      sessionStorage.removeItem(transferResultStorageKey(transactionId))
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
    processingFailureAction.value = null
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
        '송금 처리 결과를 확인할 수 없습니다. 다시 송금하지 말고 홈에서 거래 내역을 확인해 주세요.',
      )
      const failureAction = getTransferFailureAction(apiError)
      if (failureAction === 'check-status') {
        processingStatus.value = 'unknown'
        return
      }
      pendingPin.value = ''
      requestStarted.value = false
      processingStatus.value = 'error'
      processingError.value = apiError.message
      processingFailureAction.value = failureAction
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

  function appendAccountDigit(value: string) {
    if (accountNumber.value.length < 16) accountNumber.value += value
  }

  function removeAccountDigit() {
    accountNumber.value = accountNumber.value.slice(0, -1)
  }

  function appendAmountDigit(value: string) {
    const next = Number(`${amount.value || ''}${value}`)
    amount.value = next
  }

  function addAmount(value: number) {
    amount.value += value
  }

  function removeAmountDigit() {
    amount.value = Math.floor(amount.value / 10)
  }

  function reset() {
    clearStoredTransferDetail()
    recipient.value = null
    accountNumber.value = ''
    bank.value = ''
    amount.value = 0
    memo.value = ''
    balance.value = null
    bankCandidates.value = []
    processingStatus.value = 'idle'
    processingError.value = ''
    processingFailureAction.value = null
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
    processingFailureAction,
    transferIntent,
    transferResult,
    transferDetail,
    transferDetailSource,
    requestStarted,
    remainingBalance,
    isAmountOverBalance,
    canTransfer,
    setBalance,
    setTransferDetail,
    restoreTransferDetail,
    selectRecipient,
    selectManualRecipient,
    setBankCandidates,
    setVerifiedRecipient,
    createTransferIntent,
    beginTransfer,
    confirmTransferStatus,
    appendAccountDigit,
    removeAccountDigit,
    appendAmountDigit,
    addAmount,
    removeAmountDigit,
    reset,
  }
})
