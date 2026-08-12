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
  NonNullable<GuardHome['selectedWard']>['pendingApproval']
>

const props = defineProps<{
  pendingApproval: PendingApproval | null
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

const pendingDate = computed(() => {
  if (!props.pendingApproval) return ''

  const createdAt = new Date(props.pendingApproval.createdAt)
  return Number.isNaN(createdAt.getTime())
    ? ''
    : dateFormatter.format(createdAt)
})
const pendingAmount = computed(() => {
  const amount = props.pendingApproval?.amount
  return amount == null ? '-' : `-${moneyFormatter.format(amount)}원`
})
const pendingDescription = computed(
  () => props.pendingApproval?.holderName ?? '수취인 정보 없음',
)
</script>

<template>
  <section
    aria-labelledby="guard-risk-transactions-title"
    :class="pendingApproval ? 'pb-xl' : 'pb-[48px]'"
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
      v-if="!pendingApproval"
      class="mt-xl text-center text-[16px] font-medium leading-6 tracking-[-0.2px] text-gray-500"
    >
      대기중인 이상거래가 없어요.
    </p>

    <template v-else>
      <p class="type-body-medium mt-md text-gray-500">
        {{ pendingDate }}
      </p>
      <button
        class="mt-xxs flex h-[60px] w-full items-center bg-white px-sm text-left"
        type="button"
        :aria-label="`${pendingDate} ${pendingAmount} ${pendingDescription} 이상 거래 목록 보기`"
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
            {{ pendingAmount }}
          </span>
          <span
            class="mt-xxs block truncate text-[12px] font-medium leading-[1.2] tracking-[-0.24px] text-gray-700"
          >
            {{ pendingDescription }}
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
  </section>
</template>
