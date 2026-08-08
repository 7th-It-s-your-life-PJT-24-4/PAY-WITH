import { queryOptions } from '@tanstack/vue-query'
import { HTTPError } from 'ky'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import {
  getGuardTransactionDetail,
  getGuardTransactionHistory,
  type GuardTransactionHistoryParams,
} from '@/api/transactions'

export const guardTransactionKeys = {
  all: ['guard-transactions'] as const,
  lists: () => [...guardTransactionKeys.all, 'list'] as const,
  list: (wardId: number | null, params: GuardTransactionHistoryParams) =>
    [...guardTransactionKeys.lists(), wardId, params] as const,
  details: () => [...guardTransactionKeys.all, 'detail'] as const,
  detail: (wardId: number | null, transactionId: number | null) =>
    [...guardTransactionKeys.details(), wardId, transactionId] as const,
}

export function guardTransactionHistoryOptions(
  wardId: MaybeRefOrGetter<number | null>,
  params: MaybeRefOrGetter<GuardTransactionHistoryParams>,
) {
  const resolvedWardId = computed(() => toValue(wardId))
  const resolvedParams = computed(() => toValue(params))

  return queryOptions({
    queryKey: computed(() =>
      guardTransactionKeys.list(resolvedWardId.value, resolvedParams.value),
    ),
    queryFn: () =>
      getGuardTransactionHistory(resolvedWardId.value!, resolvedParams.value),
    enabled: computed(() => resolvedWardId.value !== null),
    staleTime: 15_000,
    select: (data) => ({
      ...data,
      transactions: data.transactions.filter((tx) => tx.status !== 'HELD'),
    }),
  })
}

export function guardTransactionDetailOptions(
  wardId: MaybeRefOrGetter<number | null>,
  transactionId: MaybeRefOrGetter<number | null>,
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
        resolvedWardId.value!,
        resolvedTransactionId.value!,
      ),
    enabled: computed(
      () =>
        resolvedWardId.value !== null && resolvedTransactionId.value !== null,
    ),
    staleTime: 15_000,
    retry: (failureCount, error) =>
      error instanceof HTTPError && error.response.status === 404
        ? false
        : failureCount < 1,
  })
}
