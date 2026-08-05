<script setup lang="ts">
import { useRouter } from 'vue-router'

import {
  guardTransactionCategoryIcons,
  guardTransactionStatusClasses,
  guardTransactionStatusLabels,
  type GuardTransactionCategory,
  type GuardTransactionRisk,
} from '@/pages/guard/-utils/guard-transaction-ui'

export interface GuardHistoryTransactionView {
  id: number
  date: string
  amount: string
  description: string
  category: GuardTransactionCategory
  status: GuardTransactionRisk
}

const props = defineProps<{
  wardId: number
  transactions: GuardHistoryTransactionView[]
}>()

const router = useRouter()

function openDetail(transactionId: number) {
  router.push({
    name: 'guard-transaction-detail',
    params: { transactionId },
    query: { wardId: props.wardId },
  })
}
</script>

<template>
  <section aria-label="거래 내역">
    <template
      v-for="(transaction, index) in transactions"
      :key="transaction.id"
    >
      <p
        v-if="index === 0 || transactions[index - 1]?.date !== transaction.date"
        class="type-body-medium mb-xs text-gray-500"
        :class="index > 0 ? 'mt-md' : ''"
      >
        {{ transaction.date }}
      </p>

      <button
        class="flex h-[60px] w-full items-center bg-white px-sm text-left"
        type="button"
        :aria-label="`${transaction.date} ${transaction.description} 거래 상세 보기`"
        @click="openDetail(transaction.id)"
      >
        <span
          class="flex size-8 shrink-0 items-center justify-center rounded-[10px] bg-primary-500 text-white"
        >
          <component
            :is="guardTransactionCategoryIcons[transaction.category]"
            class="size-[18px]"
            weight="fill"
            aria-hidden="true"
          />
        </span>
        <div class="ml-md min-w-0 flex-1">
          <p
            class="text-[14px] font-semibold leading-[1.2] tracking-[-0.28px] text-black"
          >
            {{ transaction.amount }}
          </p>
          <p
            class="mt-xxs truncate text-[12px] font-medium leading-[1.2] tracking-[-0.24px] text-gray-700"
          >
            {{ transaction.description }}
          </p>
        </div>
        <span
          class="rounded-small px-[6px] py-xxs text-[10px] font-bold leading-[1.2] tracking-[-0.2px]"
          :class="guardTransactionStatusClasses[transaction.status]"
        >
          {{ guardTransactionStatusLabels[transaction.status] }}
        </span>
      </button>
    </template>
  </section>
</template>
