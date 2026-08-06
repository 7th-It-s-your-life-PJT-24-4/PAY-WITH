import { queryOptions } from '@tanstack/vue-query'
import { HTTPError } from 'ky'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import { getGuardTransactionDetail } from '@/api/transactions'

export const guardTransactionKeys = {
  all: ['guard-transactions'] as const,
  details: () => [...guardTransactionKeys.all, 'detail'] as const,
  detail: (wardId: number | null, transactionId: number | null) =>
    [...guardTransactionKeys.details(), wardId, transactionId] as const,
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
