import { queryOptions } from '@tanstack/vue-query'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import { getGuardHome } from '@/api/guard-home'
import type { GuardHome } from '@/schemas/guard-home.schema'

export const guardHomeKeys = {
  all: ['guard-home'] as const,
  detail: (wardId: number | null) => [...guardHomeKeys.all, wardId] as const,
}

export function guardHomeOptions(
  wardId: MaybeRefOrGetter<number | null> = null,
) {
  const resolvedWardId = computed(() => toValue(wardId))

  return queryOptions({
    queryKey: computed(() => guardHomeKeys.detail(resolvedWardId.value)),
    queryFn: () => getGuardHome(resolvedWardId.value ?? undefined),
    staleTime: 60_000,
    gcTime: 5 * 60_000,
  })
}

export function guardPairingStatusOptions(enabled: MaybeRefOrGetter<boolean>) {
  return queryOptions<
    GuardHome,
    Error,
    GuardHome,
    ReturnType<typeof guardHomeKeys.detail>
  >({
    queryKey: guardHomeKeys.detail(null),
    queryFn: () => getGuardHome(),
    enabled: computed(() => toValue(enabled)),
    refetchInterval: 1_000,
    refetchIntervalInBackground: true,
    retry: 1,
    retryDelay: 1_000,
  })
}
