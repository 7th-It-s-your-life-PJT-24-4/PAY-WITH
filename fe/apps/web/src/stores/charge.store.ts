import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  getMockChargeAccounts,
  getMockChargeResult,
  registerMockChargeAccount,
  submitMockCharge,
} from '@/mocks/charge.mock'
import type {
  ChargeAccount,
  ChargeProcessingStatus,
  ChargeResult,
  RegisterChargeAccountRequest,
} from '@/types/charge'

export const useChargeStore = defineStore('charge', () => {
  const accounts = ref<ChargeAccount[]>([])
  const selectedAccountId = ref<number | null>(null)
  const amount = ref(0)
  const registeredAccount = ref<ChargeAccount | null>(null)
  const result = ref<ChargeResult | null>(null)
  const processingStatus = ref<ChargeProcessingStatus>('idle')
  const processingError = ref('')
  const accountsLoaded = ref(false)

  const selectedAccount = computed(
    () =>
      accounts.value.find(
        ({ accountId }) => accountId === selectedAccountId.value,
      ) ?? null,
  )
  const canCharge = computed(
    () =>
      selectedAccount.value !== null &&
      Number.isSafeInteger(amount.value) &&
      amount.value > 0 &&
      processingStatus.value !== 'pending',
  )

  async function loadAccounts() {
    accounts.value = await getMockChargeAccounts()
    accountsLoaded.value = true
    if (
      !selectedAccountId.value ||
      !accounts.value.some(
        ({ accountId }) => accountId === selectedAccountId.value,
      )
    ) {
      selectedAccountId.value = accounts.value[0]?.accountId ?? null
    }
  }

  function selectAccount(accountId: number) {
    if (accounts.value.some((account) => account.accountId === accountId))
      selectedAccountId.value = accountId
  }

  function setAmount(value: number) {
    amount.value = Math.max(0, Math.floor(value))
  }

  function addAmount(value: number) {
    setAmount(amount.value + value)
  }

  async function registerAccount(request: RegisterChargeAccountRequest) {
    processingStatus.value = 'pending'
    processingError.value = ''
    try {
      registeredAccount.value = await registerMockChargeAccount(request)
      await loadAccounts()
      selectedAccountId.value = registeredAccount.value.accountId
      processingStatus.value = 'success'
      return registeredAccount.value
    } catch (error) {
      processingStatus.value = 'error'
      processingError.value =
        error instanceof Error ? error.message : '계좌를 등록하지 못했습니다.'
      return null
    }
  }

  async function charge() {
    if (!selectedAccount.value || !canCharge.value) return null
    processingStatus.value = 'pending'
    processingError.value = ''
    try {
      result.value = await submitMockCharge(selectedAccount.value, amount.value)
      processingStatus.value = 'success'
      return result.value
    } catch (error) {
      processingStatus.value = 'error'
      processingError.value =
        error instanceof Error ? error.message : '충전을 완료하지 못했습니다.'
      return null
    }
  }

  async function restoreResult(transactionId: number) {
    try {
      result.value = await getMockChargeResult(transactionId)
      return result.value
    } catch {
      return null
    }
  }

  function resetDraft() {
    amount.value = 0
    result.value = null
    processingStatus.value = 'idle'
    processingError.value = ''
  }

  return {
    accounts,
    selectedAccountId,
    selectedAccount,
    amount,
    registeredAccount,
    result,
    processingStatus,
    processingError,
    accountsLoaded,
    canCharge,
    loadAccounts,
    selectAccount,
    setAmount,
    addAmount,
    registerAccount,
    charge,
    restoreResult,
    resetDraft,
  }
})
