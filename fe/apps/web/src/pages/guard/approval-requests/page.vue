<script setup lang="ts">
import { PhCrown } from '@phosphor-icons/vue'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { guardApprovalListOptions } from '@/lib/query/guard/approval'
import GuardApprovalHeader from '@/pages/guard/approval-requests/-components/GuardApprovalHeader.vue'
import { getApprovalDecisionSnapshots } from '@/pages/guard/approval-requests/-utils/approval-decision-snapshot'
import {
  formatApprovalDate,
  formatApprovalListAmount,
} from '@/pages/guard/approval-requests/-utils/approval-format'

type ApprovalFilter = 'pending' | 'approved' | 'rejected'
type ApprovalListItem = {
  approvalId: number
  wardId: number
  wardName: string
  amount: number
  holderName: string | null
  requestedAt: string
  status: ApprovalFilter
}

const filters: Array<{ label: string; value: ApprovalFilter }> = [
  { label: '대기', value: 'pending' },
  { label: '승인', value: 'approved' },
  { label: '거절', value: 'rejected' },
]

const route = useRoute()
const router = useRouter()
const wardId = computed(() => {
  const value = Number(route.query.wardId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const initialFilter =
  route.query.status === 'approved' || route.query.status === 'rejected'
    ? route.query.status
    : 'pending'
const activeFilter = ref<ApprovalFilter>(initialFilter)
const approvalQuery = useQuery(guardApprovalListOptions(wardId))
const processedSnapshots = getApprovalDecisionSnapshots()

const pendingItems = computed<ApprovalListItem[]>(() =>
  (approvalQuery.data.value ?? []).map((approval) => ({
    approvalId: approval.approvalId,
    wardId: approval.wardId,
    wardName: approval.wardName,
    amount: approval.amount,
    holderName: approval.holderName,
    requestedAt: approval.requestedAt,
    status: 'pending',
  })),
)
const processedItems = computed<ApprovalListItem[]>(() =>
  processedSnapshots
    .filter(
      ({ detail }) => wardId.value === null || detail.wardId === wardId.value,
    )
    .map(({ detail, decision }) => ({
      approvalId: detail.approvalId,
      wardId: detail.wardId,
      wardName: detail.wardName,
      amount: detail.amount,
      holderName: detail.holderName,
      requestedAt: detail.requestedAt,
      status:
        decision.status === 'APPROVED'
          ? ('approved' as const)
          : ('rejected' as const),
    })),
)
const approvals = computed(() =>
  activeFilter.value === 'pending'
    ? pendingItems.value
    : processedItems.value.filter(
        ({ status }) => status === activeFilter.value,
      ),
)

function isFirstOfDate(index: number) {
  const current = approvals.value[index]
  const previous = approvals.value[index - 1]
  return (
    current !== undefined &&
    (previous === undefined ||
      formatApprovalDate(current.requestedAt) !==
        formatApprovalDate(previous.requestedAt))
  )
}

function openApproval(approval: ApprovalListItem) {
  router.push({
    name:
      approval.status === 'pending'
        ? 'guard-approval-request-detail'
        : 'guard-approval-request-result',
    params: { approvalId: approval.approvalId },
    query: { wardId: wardId.value ?? undefined },
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
      @back="router.replace({ name: 'guard-home' })"
    />

    <nav class="mt-md flex gap-xs px-mobile-gutter" aria-label="이상 거래 상태">
      <button
        v-for="filter in filters"
        :key="filter.value"
        class="flex h-8 min-w-14 items-center justify-center rounded-[8px] border-[1.5px] border-primary-500 px-[15px] text-[14px] font-semibold leading-[1.6] tracking-[-0.28px] transition-colors"
        :class="
          activeFilter === filter.value
            ? 'bg-primary-500 text-white'
            : 'bg-white text-primary-500'
        "
        type="button"
        :aria-pressed="activeFilter === filter.value"
        @click="activeFilter = filter.value"
      >
        {{ filter.label }}
      </button>
    </nav>

    <section
      v-if="activeFilter === 'pending' && approvalQuery.isPending.value"
      class="flex min-h-[500px] items-center justify-center px-mobile-gutter text-center text-[16px] font-medium text-gray-500"
      aria-busy="true"
    >
      이상 거래를 불러오는 중이에요.
    </section>

    <section
      v-else-if="activeFilter === 'pending' && approvalQuery.isError.value"
      class="flex min-h-[500px] flex-col items-center justify-center px-mobile-gutter text-center"
      role="alert"
    >
      <p class="text-[16px] font-medium text-gray-700">
        이상 거래를 불러오지 못했어요.
      </p>
      <button
        class="mt-md min-h-11 px-md text-[16px] font-semibold text-primary-500"
        type="button"
        @click="approvalQuery.refetch()"
      >
        다시 시도
      </button>
    </section>

    <section
      v-else-if="approvals.length === 0"
      class="flex min-h-[500px] items-center justify-center px-mobile-gutter text-center"
    >
      <p class="text-[16px] font-medium text-gray-700">
        {{
          activeFilter === 'pending'
            ? '확인할 이상 거래가 없어요.'
            : activeFilter === 'approved'
              ? '승인한 이상 거래가 없어요.'
              : '거절한 이상 거래가 없어요.'
        }}
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
          {{ formatApprovalDate(approval.requestedAt) }}
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
