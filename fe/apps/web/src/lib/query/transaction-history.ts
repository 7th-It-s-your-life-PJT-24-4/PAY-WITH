import { queryOptions } from '@tanstack/vue-query'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import {
  getGuardTransactionHistory,
  getWardTransactionHistory,
  type GuardTransactionHistoryParams,
  type WardTransactionHistoryParams,
} from '@/api/transaction-history'

export const transactionHistoryKeys = {
  all: ['transaction-history'] as const,
  ward: (params: WardTransactionHistoryParams) =>
    [...transactionHistoryKeys.all, 'ward', params] as const,
  guard: (wardId: number | null, params: GuardTransactionHistoryParams) =>
    [...transactionHistoryKeys.all, 'guard', wardId, params] as const,
}

export function wardTransactionHistoryOptions(
  params: MaybeRefOrGetter<WardTransactionHistoryParams>,
) {
  const resolvedParams = computed(() => toValue(params))

  return queryOptions({
    queryKey: computed(() => transactionHistoryKeys.ward(resolvedParams.value)),
    queryFn: () => getWardTransactionHistory(resolvedParams.value),
  })
}

export function guardTransactionHistoryOptions(
  wardId: MaybeRefOrGetter<number | null>,
  params: MaybeRefOrGetter<GuardTransactionHistoryParams>,
) {
  const resolvedWardId = computed(() => toValue(wardId))
  const resolvedParams = computed(() => toValue(params))

  return queryOptions({
    queryKey: computed(() =>
      transactionHistoryKeys.guard(resolvedWardId.value, resolvedParams.value),
    ),
    queryFn: () => {
      if (resolvedWardId.value === null) {
        throw new Error('조회할 피보호자가 선택되지 않았습니다.')
      }
      return getGuardTransactionHistory(
        resolvedWardId.value,
        resolvedParams.value,
      )
    },
    enabled: computed(() => resolvedWardId.value !== null),
  })
}
