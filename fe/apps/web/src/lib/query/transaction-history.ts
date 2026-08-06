import { queryOptions } from '@tanstack/vue-query'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import {
  getWardTransactionHistory,
  type WardTransactionHistoryParams,
} from '@/api/transaction-history'

export const transactionHistoryKeys = {
  all: ['transaction-history'] as const,
  ward: (params: WardTransactionHistoryParams) =>
    [...transactionHistoryKeys.all, 'ward', params] as const,
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
