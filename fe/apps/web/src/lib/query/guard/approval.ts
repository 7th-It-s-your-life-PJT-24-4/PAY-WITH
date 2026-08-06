import { queryOptions } from '@tanstack/vue-query'
import { HTTPError } from 'ky'
import { computed, toValue, type MaybeRefOrGetter } from 'vue'

import {
  getApprovalRequestDetail,
  getApprovalRequestHistory,
  getApprovalRequests,
} from '@/api/approval-requests'
import type { ApprovalHistoryStatus } from '@/schemas/approval.schema'

export const guardApprovalKeys = {
  all: ['guard-approval-requests'] as const,
  lists: () => [...guardApprovalKeys.all, 'list'] as const,
  pendingList: (wardId: number | null) =>
    [...guardApprovalKeys.lists(), 'pending', wardId] as const,
  historyList: (status: ApprovalHistoryStatus | null, wardId: number | null) =>
    [...guardApprovalKeys.lists(), 'history', status, wardId] as const,
  details: () => [...guardApprovalKeys.all, 'detail'] as const,
  detail: (approvalId: number | null) =>
    [...guardApprovalKeys.details(), approvalId] as const,
}

export function guardApprovalListOptions(
  wardId: MaybeRefOrGetter<number | null> = null,
  enabled: MaybeRefOrGetter<boolean> = true,
) {
  const resolvedWardId = computed(() => toValue(wardId))

  return queryOptions({
    queryKey: computed(() =>
      guardApprovalKeys.pendingList(resolvedWardId.value),
    ),
    queryFn: () => getApprovalRequests(resolvedWardId.value ?? undefined),
    enabled: computed(() => toValue(enabled)),
    staleTime: 15_000,
  })
}

export function guardApprovalHistoryOptions(
  status: MaybeRefOrGetter<ApprovalHistoryStatus | null>,
  wardId: MaybeRefOrGetter<number | null> = null,
  enabled: MaybeRefOrGetter<boolean> = true,
) {
  const resolvedStatus = computed(() => toValue(status))
  const resolvedWardId = computed(() => toValue(wardId))

  return queryOptions({
    queryKey: computed(() =>
      guardApprovalKeys.historyList(resolvedStatus.value, resolvedWardId.value),
    ),
    queryFn: () =>
      getApprovalRequestHistory(
        resolvedStatus.value!,
        resolvedWardId.value ?? undefined,
      ),
    enabled: computed(() => resolvedStatus.value !== null && toValue(enabled)),
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
