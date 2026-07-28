<script setup lang="ts">
import {
  ArrowDownToLine,
  ArrowUpFromLine,
  CircleAlert,
  ShoppingBag,
} from '@lucide/vue'
import { computed } from 'vue'

import {
  formatTransactionAmount,
  formatTransactionTime,
  getTransactionTypeLabel,
  transactionRiskLabel,
} from '@/pages/ward/history/-utils/transaction-format'
import type { WardTransaction } from '@/types/transaction'

const props = defineProps<{
  transaction: WardTransaction
}>()

const icon = computed(() => {
  if (props.transaction.riskLevel === 'BLOCKED') return CircleAlert
  if (props.transaction.type === 'PAYMENT') return ShoppingBag
  return props.transaction.direction === 'CREDIT'
    ? ArrowDownToLine
    : ArrowUpFromLine
})

const iconClass = computed(
  () =>
    ({
      SAFE: 'bg-primary-900 text-primary-300',
      CAUTION: 'bg-warning/10 text-warning',
      BLOCKED: 'bg-error/10 text-error',
    })[props.transaction.riskLevel],
)

const riskClass = computed(
  () =>
    ({
      SAFE: 'bg-success/10 text-success',
      CAUTION: 'bg-warning/10 text-warning',
      BLOCKED: 'bg-error/10 text-error',
    })[props.transaction.riskLevel],
)
</script>

<template>
  <RouterLink
    :to="{
      name: 'ward-transaction-detail',
      params: { transactionId: transaction.transactionId },
    }"
    class="flex min-h-[96px] items-center gap-md rounded-large border bg-surface-card p-md shadow-card outline-none focus-visible:ring-2 focus-visible:ring-focus"
    :class="
      transaction.riskLevel === 'BLOCKED' ? 'border-error/30' : 'border-border'
    "
    :aria-label="`${transaction.title} ${formatTransactionAmount(transaction.amount, transaction.direction)} 상세 보기`"
  >
    <span
      class="flex size-14 shrink-0 items-center justify-center rounded-full"
      :class="iconClass"
      aria-hidden="true"
    >
      <component :is="icon" class="size-7" :stroke-width="2" />
    </span>

    <span class="min-w-0 flex-1">
      <span class="flex items-start justify-between gap-sm">
        <strong
          class="type-h3 truncate"
          :class="
            transaction.riskLevel === 'BLOCKED' ? 'text-error' : 'text-body'
          "
        >
          {{ transaction.title }}
        </strong>
        <strong
          class="type-h3 shrink-0 font-number"
          :class="
            transaction.riskLevel === 'BLOCKED'
              ? 'text-error'
              : transaction.direction === 'CREDIT'
                ? 'text-primary-300'
                : 'text-body'
          "
        >
          {{
            formatTransactionAmount(transaction.amount, transaction.direction)
          }}
        </strong>
      </span>

      <span class="mt-xs flex items-center justify-between gap-sm">
        <span class="type-caption min-w-0 truncate text-body-muted">
          <span class="font-number">{{
            formatTransactionTime(transaction.occurredAt)
          }}</span>
          <span aria-hidden="true"> · </span>
          {{ getTransactionTypeLabel(transaction.type, transaction.direction) }}
        </span>
        <span
          class="type-caption shrink-0 rounded-full px-sm py-xxs font-medium"
          :class="riskClass"
        >
          {{ transactionRiskLabel[transaction.riskLevel] }}
        </span>
      </span>
    </span>
  </RouterLink>
</template>
