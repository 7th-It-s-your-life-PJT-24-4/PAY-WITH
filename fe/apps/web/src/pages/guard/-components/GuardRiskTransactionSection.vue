<script setup lang="ts">
import { ChevronRight } from '@lucide/vue'
import { computed } from 'vue'

import {
  guardTransactionCategoryIcons,
  guardTransactionStatusClasses,
  guardTransactionStatusLabels,
} from '@/pages/guard/-utils/guard-transaction-ui'
import type { GuardHome } from '@/schemas/guard-home.schema'

type PendingApproval = NonNullable<
  GuardHome['selectedWard']
>['pendingApprovals'][number]

const props = defineProps<{
  pendingApprovals: PendingApproval[]
}>()

const emit = defineEmits<{
  more: []
}>()

const moneyFormatter = new Intl.NumberFormat('ko-KR')
const dateFormatter = new Intl.DateTimeFormat('ko-KR', {
  month: 'long',
  day: 'numeric',
})
const pendingIcon = guardTransactionCategoryIcons.transfer
const displayedPendingApprovals = computed(() =>
  props.pendingApprovals.slice(0, 3),
)

function formatPendingDate(pendingApproval: PendingApproval) {
  const createdAt = new Date(pendingApproval.createdAt)
  return Number.isNaN(createdAt.getTime())
    ? ''
    : dateFormatter.format(createdAt)
}

function formatPendingAmount(pendingApproval: PendingApproval) {
  const { amount } = pendingApproval
  return amount == null ? '-' : `-${moneyFormatter.format(amount)}원`
}

function getPendingDescription(pendingApproval: PendingApproval) {
  return pendingApproval.holderName ?? '수취인 정보 없음'
}
</script>

<template>
  <section
    aria-labelledby="guard-risk-transactions-title"
    :class="displayedPendingApprovals.length > 0 ? 'pb-xl' : 'pb-[48px]'"
  >
    <button
      class="flex w-full items-center justify-between text-left"
      type="button"
      aria-label="이상 거래 내역 더보기"
      @click="emit('more')"
    >
      <h2
        id="guard-risk-transactions-title"
        class="text-[18px] font-bold leading-[1.2] tracking-[-0.36px] text-black"
      >
        이상 거래 내역
      </h2>
      <ChevronRight class="size-6 text-gray-700" aria-hidden="true" />
    </button>

    <p
      v-if="displayedPendingApprovals.length === 0"
      class="mt-xl text-center text-[16px] font-medium leading-6 tracking-[-0.2px] text-gray-500"
    >
      대기중인 이상거래가 없어요.
    </p>

    <div v-else class="mt-md">
      <template
        v-for="(pendingApproval, index) in displayedPendingApprovals"
        :key="pendingApproval.transactionId"
      >
        <p
          v-if="
            index === 0 ||
            formatPendingDate(displayedPendingApprovals[index - 1]!) !==
              formatPendingDate(pendingApproval)
          "
          class="type-body-medium text-gray-500"
          :class="index > 0 ? 'mt-md' : ''"
        >
          {{ formatPendingDate(pendingApproval) }}
        </p>
        <button
          class="mt-xxs flex h-[60px] w-full items-center bg-white px-sm text-left"
          type="button"
          :aria-label="`${formatPendingDate(pendingApproval)} ${formatPendingAmount(pendingApproval)} ${getPendingDescription(pendingApproval)} 이상 거래 목록 보기`"
          @click="emit('more')"
        >
          <span
            class="flex size-8 shrink-0 items-center justify-center rounded-[10px] bg-primary-500 text-white"
          >
            <component
              :is="pendingIcon"
              class="size-[18px]"
              weight="fill"
              aria-hidden="true"
            />
          </span>
          <span class="ml-md min-w-0 flex-1">
            <span
              class="block text-[14px] font-semibold leading-[1.2] tracking-[-0.28px] text-black"
            >
              {{ formatPendingAmount(pendingApproval) }}
            </span>
            <span
              class="mt-xxs block truncate text-[12px] font-medium leading-[1.2] tracking-[-0.24px] text-gray-700"
            >
              {{ getPendingDescription(pendingApproval) }}
            </span>
          </span>
          <span
            class="rounded-small px-[6px] py-xxs text-[10px] font-bold leading-[1.2] tracking-[-0.2px]"
            :class="guardTransactionStatusClasses.danger"
          >
            {{ guardTransactionStatusLabels.danger }}
          </span>
        </button>
      </template>
    </div>
  </section>
</template>
