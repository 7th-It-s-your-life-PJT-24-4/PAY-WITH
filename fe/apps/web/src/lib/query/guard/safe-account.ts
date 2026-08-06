import { queryOptions } from '@tanstack/vue-query'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import { getGuardSafeAccounts } from '@/api/guard-safe-accounts'

export const guardSafeAccountKeys = {
  all: ['guard-safe-accounts'] as const,
  lists: () => [...guardSafeAccountKeys.all, 'list'] as const,
  list: (wardId: number | null) =>
    [...guardSafeAccountKeys.lists(), wardId] as const,
}

export function guardSafeAccountsOptions(
  wardId: MaybeRefOrGetter<number | null>,
) {
  const resolvedWardId = computed(() => toValue(wardId))

  return queryOptions({
    queryKey: computed(() => guardSafeAccountKeys.list(resolvedWardId.value)),
    queryFn: () => getGuardSafeAccounts(resolvedWardId.value!),
    enabled: computed(() => resolvedWardId.value !== null),
    staleTime: 15_000,
  })
}
