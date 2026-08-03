import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { tokenStorage } from '@/api/token-storage'
import type { Account } from '@/schemas/account.schema'
import { chargeResultSchema, type ChargeResult } from '@/schemas/charge.schema'

// 완료 화면 새로고침 시 충전 결과를 복원하되,
// JWT 사용자별 저장 키로 분리해 다른 계정의 결과가 노출되지 않도록 한다.
const resultStorageKeyPrefix = 'pay-with:ward-charge-result'

function getResultStorageKey() {
  try {
    const accessToken = tokenStorage.getAccessToken()
    if (!accessToken) return `${resultStorageKeyPrefix}:anonymous`
    const payload = accessToken.split('.')[1]
    if (!payload) return `${resultStorageKeyPrefix}:anonymous`
    const base64Payload = payload.replace(/-/g, '+').replace(/_/g, '/')
    const normalizedPayload = base64Payload.padEnd(
      base64Payload.length + ((4 - (base64Payload.length % 4)) % 4),
      '=',
    )
    const subject = JSON.parse(atob(normalizedPayload)) as { sub?: unknown }
    return typeof subject.sub === 'string' && subject.sub.length > 0
      ? `${resultStorageKeyPrefix}:${subject.sub}`
      : `${resultStorageKeyPrefix}:anonymous`
  } catch {
    return `${resultStorageKeyPrefix}:anonymous`
  }
}

export const useChargeStore = defineStore('charge', () => {
  const selectedAccountId = ref<number | null>(null)
  const amount = ref(0)
  const registeredAccount = ref<Account | null>(null)
  const result = ref<ChargeResult | null>(null)
  const resultStorageKey = ref<string | null>(null)

  const canCharge = computed(
    () =>
      selectedAccountId.value !== null &&
      Number.isSafeInteger(amount.value) &&
      amount.value > 0,
  )

  function syncAccounts(accounts: Account[]) {
    if (
      selectedAccountId.value === null ||
      !accounts.some(({ accountId }) => accountId === selectedAccountId.value)
    ) {
      selectedAccountId.value = accounts[0]?.accountId ?? null
    }
  }

  function selectAccount(accountId: number) {
    selectedAccountId.value = accountId
  }

  function setAmount(value: number) {
    amount.value = Math.max(0, Math.floor(value))
  }

  function addAmount(value: number) {
    setAmount(amount.value + value)
  }

  function saveRegisteredAccount(account: Account) {
    registeredAccount.value = account
    selectedAccountId.value = account.accountId
  }

  function saveResult(value: ChargeResult) {
    result.value = value
    resultStorageKey.value = getResultStorageKey()
    try {
      sessionStorage.setItem(resultStorageKey.value, JSON.stringify(value))
    } catch {
      // 저장소를 사용할 수 없어도 완료된 충전 흐름은 유지한다.
    }
  }

  function restoreResult(transactionId: number) {
    const currentStorageKey = getResultStorageKey()
    if (
      resultStorageKey.value === currentStorageKey &&
      result.value?.transactionId === transactionId
    )
      return result.value
    if (resultStorageKey.value !== currentStorageKey) {
      result.value = null
      resultStorageKey.value = null
    }

    try {
      const saved: unknown = JSON.parse(
        sessionStorage.getItem(currentStorageKey) ?? 'null',
      )
      const parsed = chargeResultSchema.safeParse(saved)
      if (!parsed.success || parsed.data.transactionId !== transactionId)
        return null
      result.value = parsed.data
      resultStorageKey.value = currentStorageKey
      return result.value
    } catch {
      return null
    }
  }

  function resetDraft() {
    amount.value = 0
    result.value = null
    resultStorageKey.value = null
    try {
      sessionStorage.removeItem(getResultStorageKey())
    } catch {
      // 저장소를 사용할 수 없는 환경에서는 메모리 상태만 초기화한다.
    }
  }

  return {
    selectedAccountId,
    amount,
    registeredAccount,
    result,
    canCharge,
    syncAccounts,
    selectAccount,
    setAmount,
    addAmount,
    saveRegisteredAccount,
    saveResult,
    restoreResult,
    resetDraft,
  }
})
