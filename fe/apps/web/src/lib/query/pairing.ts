import { queryOptions } from '@tanstack/vue-query'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import {
  getPendingPairingRequest,
  getWardPairingRequestStatus,
} from '@/api/pairing'

export const pairingKeys = {
  all: ['pairing'] as const,
  pendingRequest: () => [...pairingKeys.all, 'guard-pending-request'] as const,
  requestStatus: (requestId: string | null) =>
    [...pairingKeys.all, 'request-status', requestId] as const,
}

export function pendingPairingRequestOptions() {
  return queryOptions({
    queryKey: pairingKeys.pendingRequest(),
    queryFn: getPendingPairingRequest,
    refetchInterval: 2_000,
    refetchIntervalInBackground: true,
    refetchOnWindowFocus: 'always',
    retry: 1,
  })
}

export function wardPairingRequestStatusOptions(
  requestId: MaybeRefOrGetter<string | null>,
) {
  const resolvedRequestId = computed(() => toValue(requestId))

  return queryOptions({
    queryKey: computed(() =>
      pairingKeys.requestStatus(resolvedRequestId.value),
    ),
    queryFn: () => getWardPairingRequestStatus(resolvedRequestId.value ?? ''),
    enabled: computed(() => Boolean(resolvedRequestId.value)),
    refetchInterval: 2_000,
    refetchIntervalInBackground: true,
    refetchOnWindowFocus: 'always',
    retry: 1,
  })
}
