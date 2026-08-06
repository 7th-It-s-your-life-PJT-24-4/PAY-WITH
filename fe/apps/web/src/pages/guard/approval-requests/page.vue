<script setup lang="ts">
import { PhCrown } from '@phosphor-icons/vue'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  guardApprovalHistoryOptions,
  guardApprovalListOptions,
} from '@/lib/query/guard/approval'
import GuardApprovalHeader from '@/pages/guard/approval-requests/-components/GuardApprovalHeader.vue'
import {
  formatApprovalDate,
  formatApprovalListAmount,
} from '@/pages/guard/approval-requests/-utils/approval-format'
import {
  parsePositiveRouteId,
  withGuardWardId,
} from '@/pages/guard/-utils/guard-route'
import type {
  ApprovalHistoryStatus,
  ApprovalRequestSummary,
} from '@/schemas/approval.schema'

type ApprovalFilter =
  'pending' | 'approved' | 'rejected' | 'canceled' | 'expired'

const filters: Array<{ label: string; value: ApprovalFilter }> = [
  { label: '대기', value: 'pending' },
  { label: '승인', value: 'approved' },
  { label: '거절', value: 'rejected' },
  { label: '취소', value: 'canceled' },
  { label: '만료', value: 'expired' },
]

const historyStatusByFilter: Partial<
  Record<ApprovalFilter, ApprovalHistoryStatus>
> = {
  approved: 'APPROVED',
  rejected: 'REJECTED',
  canceled: 'CANCELED',
  expired: 'EXPIRED',
}

const emptyMessageByFilter: Record<ApprovalFilter, string> = {
  pending: '확인할 이상 거래가 없어요.',
  approved: '승인한 이상 거래가 없어요.',
  rejected: '거절한 이상 거래가 없어요.',
  canceled: '취소한 이상 거래가 없어요.',
  expired: '만료된 이상 거래가 없어요.',
}

const route = useRoute()
const router = useRouter()
const wardId = computed(() => parsePositiveRouteId(route.query.wardId))
const initialFilter = filters.some(({ value }) => value === route.query.status)
  ? (route.query.status as ApprovalFilter)
  : 'pending'
const activeFilter = ref<ApprovalFilter>(initialFilter)
const activeHistoryStatus = computed(
  () => historyStatusByFilter[activeFilter.value] ?? null,
)
const pendingQuery = useQuery(
  guardApprovalListOptions(
    wardId,
    computed(() => activeFilter.value === 'pending'),
  ),
)
const historyQuery = useQuery(
  guardApprovalHistoryOptions(
    activeHistoryStatus,
    wardId,
    computed(() => activeFilter.value !== 'pending'),
  ),
)

const approvals = computed<ApprovalRequestSummary[]>(() =>
  activeFilter.value === 'pending'
    ? (pendingQuery.data.value ?? [])
    : (historyQuery.data.value ?? []),
)
const isLoading = computed(() =>
  activeFilter.value === 'pending'
    ? pendingQuery.isPending.value
    : historyQuery.isPending.value,
)
const isError = computed(() =>
  activeFilter.value === 'pending'
    ? pendingQuery.isError.value
    : historyQuery.isError.value,
)

function selectFilter(filter: ApprovalFilter) {
  activeFilter.value = filter
  router.replace({
    query: {
      ...route.query,
      status: filter === 'pending' ? undefined : filter,
    },
  })
}

function refetchActiveList() {
  if (activeFilter.value === 'pending') {
    pendingQuery.refetch()
    return
  }
  historyQuery.refetch()
}

function displayDate(approval: ApprovalRequestSummary) {
  return approval.respondedAt ?? approval.requestedAt
}

function isFirstOfDate(index: number) {
  const current = approvals.value[index]
  const previous = approvals.value[index - 1]
  return (
    current !== undefined &&
    (previous === undefined ||
      formatApprovalDate(displayDate(current)) !==
        formatApprovalDate(displayDate(previous)))
  )
}

function openApproval(approval: ApprovalRequestSummary) {
  if (approval.status === 'PENDING') {
    router.push({
      name: 'guard-approval-request-detail',
      params: { id: approval.approvalId },
      query: { wardId: approval.wardId },
    })
    return
  }

  router.push({
    name: 'guard-transaction-detail',
    params: { id: approval.transactionId },
    query: {
      wardId: approval.wardId,
      status: approval.status.toLowerCase(),
      source: 'approval',
    },
  })
}

function goHome() {
  router.replace({
    name: 'guard-home',
    query: withGuardWardId({}, wardId.value),
  })
}
</script>

<template>
  <main
    class="min-h-screen bg-white pb-[calc(66px+env(safe-area-inset-bottom))]"
  >
    <GuardApprovalHeader
      title="이상 거래 목록"
      back-label="보호자 홈으로 돌아가기"
      @back="goHome"
    />

    <nav
      class="mt-md flex gap-xs overflow-x-auto px-mobile-gutter"
      aria-label="이상 거래 상태"
    >
      <button
        v-for="filter in filters"
        :key="filter.value"
        class="flex h-8 min-w-14 shrink-0 items-center justify-center rounded-[8px] border-[1.5px] border-primary-500 px-[15px] text-[14px] font-semibold leading-[1.6] tracking-[-0.28px] transition-colors"
        :class="
          activeFilter === filter.value
            ? 'bg-primary-500 text-white'
            : 'bg-white text-primary-500'
        "
        type="button"
        :aria-pressed="activeFilter === filter.value"
        @click="selectFilter(filter.value)"
      >
        {{ filter.label }}
      </button>
    </nav>

    <section
      v-if="isLoading"
      class="flex min-h-[500px] items-center justify-center px-mobile-gutter text-center text-[16px] font-medium text-gray-500"
      aria-busy="true"
    >
      이상 거래를 불러오는 중이에요.
    </section>

    <section
      v-else-if="isError"
      class="flex min-h-[500px] flex-col items-center justify-center px-mobile-gutter text-center"
      role="alert"
    >
      <p class="text-[16px] font-medium text-gray-700">
        이상 거래를 불러오지 못했어요.
      </p>
      <button
        class="mt-md min-h-11 px-md text-[16px] font-semibold text-primary-500"
        type="button"
        @click="refetchActiveList"
      >
        다시 시도
      </button>
    </section>

    <section
      v-else-if="approvals.length === 0"
      class="flex min-h-[500px] items-center justify-center px-mobile-gutter text-center"
    >
      <p class="text-[16px] font-medium text-gray-700">
        {{ emptyMessageByFilter[activeFilter] }}
      </p>
    </section>

    <section v-else class="mt-md px-mobile-gutter" aria-label="이상 거래 목록">
      <template
        v-for="(approval, index) in approvals"
        :key="approval.approvalId"
      >
        <p
          v-if="isFirstOfDate(index)"
          class="mb-xs text-[14px] font-medium leading-[1.2] tracking-[-0.28px] text-gray-500"
          :class="index > 0 ? 'mt-md' : ''"
        >
          {{ formatApprovalDate(displayDate(approval)) }}
        </p>

        <button
          class="flex h-[60px] w-full items-center bg-white px-sm text-left"
          type="button"
          :aria-label="`${approval.wardName}님의 ${formatApprovalListAmount(approval.amount)} 이상 거래 상세 보기`"
          @click="openApproval(approval)"
        >
          <span
            class="flex size-8 shrink-0 items-center justify-center rounded-[10px] bg-primary-500 text-white"
            aria-hidden="true"
          >
            <PhCrown class="size-[18px]" weight="fill" />
          </span>
          <div class="ml-md min-w-0 flex-1">
            <p
              class="text-[14px] font-semibold leading-[1.2] tracking-[-0.28px] text-black"
            >
              {{ formatApprovalListAmount(approval.amount) }}
            </p>
            <p
              class="mt-xxs truncate text-[12px] font-medium leading-[1.2] tracking-[-0.24px] text-gray-700"
            >
              {{ approval.wardName }} ·
              {{ approval.holderName ?? '받는 분 미상' }}
            </p>
          </div>
          <span
            class="rounded-small bg-[#fff3f3] px-[6px] py-xxs text-[10px] font-bold leading-[1.2] tracking-[-0.2px] text-error"
          >
            위험
          </span>
        </button>
      </template>
    </section>
  </main>
</template>
