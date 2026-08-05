import { queryOptions } from '@tanstack/vue-query'
import { HTTPError } from 'ky'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import { getWardApprovalDetail, getWardHome } from '@/api/home'
import { getApiErrorCode } from '@/api/error'

export const wardHomeKeys = {
  all: ['ward-home'] as const,
  approval: (approvalId: number | null) =>
    [...wardHomeKeys.all, 'approval', approvalId] as const,
}

export function wardHomeOptions(enabled: MaybeRefOrGetter<boolean> = true) {
  return queryOptions({
    queryKey: wardHomeKeys.all,
    queryFn: getWardHome,
    enabled: computed(() => toValue(enabled)),
    retry: (failureCount, error) =>
      getApiErrorCode(error) === 'WARD_001' ? false : failureCount < 1,
  })
}

export function wardApprovalDetailOptions(
  approvalId: MaybeRefOrGetter<number | null>,
) {
  const resolvedApprovalId = computed(() => toValue(approvalId))

  return queryOptions({
    queryKey: computed(() => wardHomeKeys.approval(resolvedApprovalId.value)),
    queryFn: () => getWardApprovalDetail(resolvedApprovalId.value!),
    enabled: computed(() => resolvedApprovalId.value !== null),
    retry: (failureCount, error) =>
      error instanceof HTTPError && error.response.status === 404
        ? false
        : failureCount < 1,
    refetchInterval: (query) => (query.state.error ? false : 3_000),
    refetchOnWindowFocus: true,
  })
}
