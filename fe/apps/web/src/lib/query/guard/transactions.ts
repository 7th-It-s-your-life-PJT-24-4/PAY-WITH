import { queryOptions } from '@tanstack/vue-query'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import {
  getGuardTransactionDetail,
  getGuardTransactionHistory,
} from '@/api/guard-transactions'
import type { TransactionRiskLevel } from '@/schemas/guard-transaction.schema'

export const guardTransactionKeys = {
  all: ['guard-transactions'] as const,
  history: (wardId: number | null, riskLevel: TransactionRiskLevel | null) =>
    [...guardTransactionKeys.all, 'history', wardId, riskLevel] as const,
  detail: (wardId: number, transactionId: number) =>
    [...guardTransactionKeys.all, 'detail', wardId, transactionId] as const,
}

export function guardTransactionHistoryOptions(
  wardId: MaybeRefOrGetter<number | null>,
  riskLevel: MaybeRefOrGetter<TransactionRiskLevel | null>,
) {
  const resolvedWardId = computed(() => toValue(wardId))
  const resolvedRiskLevel = computed(() => toValue(riskLevel))

  return queryOptions({
    queryKey: computed(() =>
      guardTransactionKeys.history(
        resolvedWardId.value,
        resolvedRiskLevel.value,
      ),
    ),
    queryFn: () =>
      getGuardTransactionHistory(
        resolvedWardId.value!,
        resolvedRiskLevel.value ?? undefined,
      ),
    enabled: computed(
      () =>
        Number.isSafeInteger(resolvedWardId.value) && resolvedWardId.value! > 0,
    ),
  })
}

export function guardTransactionDetailOptions(
  wardId: MaybeRefOrGetter<number>,
  transactionId: MaybeRefOrGetter<number>,
) {
  const resolvedWardId = computed(() => toValue(wardId))
  const resolvedTransactionId = computed(() => toValue(transactionId))

  return queryOptions({
    queryKey: computed(() =>
      guardTransactionKeys.detail(
        resolvedWardId.value,
        resolvedTransactionId.value,
      ),
    ),
    queryFn: () =>
      getGuardTransactionDetail(
        resolvedWardId.value,
        resolvedTransactionId.value,
      ),
    enabled: computed(
      () =>
        Number.isSafeInteger(resolvedWardId.value) &&
        resolvedWardId.value > 0 &&
        Number.isSafeInteger(resolvedTransactionId.value) &&
        resolvedTransactionId.value > 0,
    ),
  })
}
