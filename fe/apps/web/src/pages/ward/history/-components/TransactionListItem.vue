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
  transactionRiskLabel,
} from '@/pages/ward/history/-utils/transaction-format'
import type { WardTransactionHistoryItem } from '@/schemas/transaction-history.schema'

const props = defineProps<{
  transaction: WardTransactionHistoryItem
}>()

const icon = computed(() => {
  if (props.transaction.riskLevel === 'DANGER') return CircleAlert
  if (props.transaction.type === 'PAYMENT') return ShoppingBag
  return props.transaction.direction === 'IN'
    ? ArrowDownToLine
    : ArrowUpFromLine
})

const iconClass = computed(() =>
  props.transaction.riskLevel
    ? {
        SAFE: 'bg-primary-900 text-primary-300',
        CAUTION: 'bg-warning/10 text-warning',
        DANGER: 'bg-error/10 text-error',
      }[props.transaction.riskLevel]
    : 'bg-gray-100 text-body-muted',
)

const riskClass = computed(() =>
  props.transaction.riskLevel
    ? {
        SAFE: 'bg-success/10 text-success',
        CAUTION: 'bg-warning/10 text-warning',
        DANGER: 'bg-error/10 text-error',
      }[props.transaction.riskLevel]
    : '',
)

const typeLabel = computed(() => {
  if (props.transaction.type === 'CHARGE') return '충전'
  if (props.transaction.type === 'PAYMENT') return '결제'
  return '송금'
})
</script>

<template>
  <article
    class="flex min-h-[96px] items-center gap-md rounded-large border bg-surface-card p-md shadow-card"
    :class="
      transaction.riskLevel === 'DANGER' ? 'border-error/30' : 'border-border'
    "
    :aria-label="`${transaction.title} ${formatTransactionAmount(transaction.amount, transaction.direction)}`"
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
            transaction.riskLevel === 'DANGER' ? 'text-error' : 'text-body'
          "
        >
          {{ transaction.title }}
        </strong>
        <strong
          class="type-h3 shrink-0 font-number"
          :class="
            transaction.riskLevel === 'DANGER'
              ? 'text-error'
              : transaction.direction === 'IN'
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
          {{ typeLabel }}
        </span>
        <span
          v-if="transaction.riskLevel"
          class="type-caption shrink-0 rounded-full px-sm py-xxs font-medium"
          :class="riskClass"
        >
          {{
            transaction.status === 'BLOCKED'
              ? '거래 차단됨'
              : transactionRiskLabel[transaction.riskLevel]
          }}
        </span>
      </span>
    </span>
  </article>
</template>
