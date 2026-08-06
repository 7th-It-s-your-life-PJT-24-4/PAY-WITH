<script setup lang="ts">
import {
  ArrowDownToLine,
  ArrowUpFromLine,
  CircleAlert,
  ShoppingBag,
} from '@lucide/vue'
import { computed } from 'vue'
import { RouterLink } from 'vue-router'

import {
  formatTransactionAmount,
  formatTransactionTime,
  transactionRiskLabel,
} from '@/pages/ward/history/-utils/transaction-format'
import type { WardTransactionHistoryItem } from '@/schemas/transaction-history.schema'

const props = defineProps<{
  transaction: WardTransactionHistoryItem
}>()

type InterruptedStatus = 'BLOCKED' | 'REJECTED' | 'CANCELED' | 'FAILED'

const interruptedStatusLabel: Record<InterruptedStatus, string> = {
  BLOCKED: '거래 차단됨',
  REJECTED: '거래 거절됨',
  CANCELED: '거래 취소됨',
  FAILED: '거래 실패',
}

const interruptedStatuses = new Set<string>([
  'BLOCKED',
  'REJECTED',
  'CANCELED',
  'FAILED',
])

const isInterruptedTransaction = computed(() =>
  interruptedStatuses.has(props.transaction.status),
)

const statusBadgeLabel = computed(() => {
  if (!isInterruptedTransaction.value) return null
  return interruptedStatusLabel[props.transaction.status as InterruptedStatus]
})

const icon = computed(() => {
  if (isInterruptedTransaction.value) return CircleAlert
  if (props.transaction.riskLevel === 'DANGER') return CircleAlert
  if (props.transaction.type === 'PAYMENT') return ShoppingBag
  return props.transaction.direction === 'IN'
    ? ArrowDownToLine
    : ArrowUpFromLine
})

const iconClass = computed(() =>
  isInterruptedTransaction.value
    ? 'bg-gray-900 text-body-muted'
    : props.transaction.riskLevel
      ? {
          SAFE: 'bg-primary-900 text-primary-300',
          CAUTION: 'bg-warning/10 text-warning',
          DANGER: 'bg-error/10 text-error',
        }[props.transaction.riskLevel]
      : 'bg-primary-900 text-body-muted',
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

const badgeClass = computed(() =>
  isInterruptedTransaction.value
    ? 'bg-gray-900 text-body-secondary'
    : riskClass.value,
)

const typeLabel = computed(() => {
  if (props.transaction.type === 'CHARGE') return '충전'
  if (props.transaction.type === 'PAYMENT') return '결제'
  return '송금'
})
</script>

<template>
  <RouterLink
    :to="{
      name: 'ward-transaction-detail',
      params: { transactionId: String(transaction.transactionId) },
    }"
    class="flex min-h-[96px] items-center gap-md rounded-large border bg-surface-card p-md shadow-card"
    :class="
      isInterruptedTransaction
        ? 'border-gray-900'
        : transaction.riskLevel === 'DANGER'
          ? 'border-error/30'
          : 'border-border'
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
          :class="[
            isInterruptedTransaction
              ? 'text-body-muted'
              : transaction.riskLevel === 'DANGER'
                ? 'text-error'
                : 'text-body',
            isInterruptedTransaction ? 'line-through decoration-2' : '',
          ]"
        >
          {{ transaction.title }}
        </strong>
        <strong
          class="type-h3 shrink-0 font-number"
          :class="[
            isInterruptedTransaction
              ? 'text-body-muted'
              : transaction.riskLevel === 'DANGER'
                ? 'text-error'
                : transaction.direction === 'IN'
                  ? 'text-primary-300'
                  : 'text-body',
            isInterruptedTransaction ? 'line-through decoration-2' : '',
          ]"
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
          v-if="statusBadgeLabel || transaction.riskLevel"
          class="type-caption shrink-0 rounded-full px-sm py-xxs font-medium"
          :class="badgeClass"
        >
          {{
            statusBadgeLabel ??
            (transaction.riskLevel &&
              transactionRiskLabel[transaction.riskLevel])
          }}
        </span>
      </span>
    </span>
  </RouterLink>
</template>
