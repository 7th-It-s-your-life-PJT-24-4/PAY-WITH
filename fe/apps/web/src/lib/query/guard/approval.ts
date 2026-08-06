import { queryOptions } from '@tanstack/vue-query'
import { HTTPError } from 'ky'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import {
  getApprovalRequestDetail,
  getApprovalRequests,
} from '@/api/approval-requests'

export const guardApprovalKeys = {
  all: ['guard-approval-requests'] as const,
  lists: () => [...guardApprovalKeys.all, 'list'] as const,
  list: (wardId: number | null) =>
    [...guardApprovalKeys.lists(), wardId] as const,
  details: () => [...guardApprovalKeys.all, 'detail'] as const,
  detail: (approvalId: number | null) =>
    [...guardApprovalKeys.details(), approvalId] as const,
}

export function guardApprovalListOptions(
  wardId: MaybeRefOrGetter<number | null> = null,
) {
  const resolvedWardId = computed(() => toValue(wardId))

  return queryOptions({
    queryKey: computed(() => guardApprovalKeys.list(resolvedWardId.value)),
    queryFn: () => getApprovalRequests(resolvedWardId.value ?? undefined),
    staleTime: 15_000,
  })
}

export function guardApprovalDetailOptions(
  approvalId: MaybeRefOrGetter<number | null>,
  enabled: MaybeRefOrGetter<boolean> = true,
) {
  const resolvedApprovalId = computed(() => toValue(approvalId))

  return queryOptions({
    queryKey: computed(() =>
      guardApprovalKeys.detail(resolvedApprovalId.value),
    ),
    queryFn: () => getApprovalRequestDetail(resolvedApprovalId.value!),
    enabled: computed(
      () => resolvedApprovalId.value !== null && toValue(enabled),
    ),
    retry: (failureCount, error) =>
      error instanceof HTTPError &&
      (error.response.status === 404 || error.response.status === 409)
        ? false
        : failureCount < 1,
  })
}
