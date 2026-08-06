<script setup lang="ts">
import { ArrowUpFromLine, CircleAlert } from '@lucide/vue'

import type { PendingApprovalItem } from '@/schemas/home.schema'

defineProps<{
  transactions: PendingApprovalItem[]
}>()

const emit = defineEmits<{
  select: [transaction: PendingApprovalItem]
}>()

const riskClass: Record<
  NonNullable<PendingApprovalItem['riskLevel']>,
  string
> = {
  SAFE: 'bg-success/10 text-success',
  CAUTION: 'bg-warning/10 text-warning',
  DANGER: 'bg-error/10 text-error',
}

function formatAmount(amount: number) {
  return `${amount.toLocaleString('ko-KR')}원`
}

function formatTime(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(value))
}

function getRiskLabel(riskLevel: PendingApprovalItem['riskLevel']) {
  if (riskLevel === 'DANGER') return '위험'
  if (riskLevel === 'CAUTION') return '주의'
  return '확인 필요'
}

function getAccessibleLabel(transaction: PendingApprovalItem) {
  return `송금 ${transaction.holderName} 님에게 ${formatAmount(transaction.amount)} 상세 확인`
}
</script>

<template>
  <ul v-if="transactions.length" class="grid gap-md">
    <li v-for="transaction in transactions" :key="transaction.approvalId">
      <button
        type="button"
        class="flex min-h-[96px] w-full items-center gap-md rounded-large border bg-surface-card p-md text-left shadow-card outline-none transition-colors hover:bg-primary-900/50 focus-visible:ring-2 focus-visible:ring-focus"
        :class="
          transaction.riskLevel === 'DANGER'
            ? 'border-error/30'
            : 'border-border'
        "
        :aria-label="getAccessibleLabel(transaction)"
        @click="emit('select', transaction)"
      >
        <span
          class="flex size-14 shrink-0 items-center justify-center rounded-full"
          :class="
            transaction.riskLevel === 'DANGER'
              ? 'bg-error/10 text-error'
              : 'bg-warning/10 text-warning'
          "
          aria-hidden="true"
        >
          <CircleAlert
            v-if="transaction.riskLevel === 'DANGER'"
            class="size-7"
            :stroke-width="2"
          />
          <ArrowUpFromLine v-else class="size-7" :stroke-width="2" />
        </span>

        <span class="min-w-0 flex-1">
          <span class="flex items-start justify-between gap-sm">
            <strong class="type-h3 truncate text-body">
              {{ transaction.holderName }} 님에게
            </strong>
            <strong class="type-h3 shrink-0 font-number text-body">
              {{ formatAmount(transaction.amount) }}
            </strong>
          </span>

          <span class="mt-xs flex items-center justify-between gap-sm">
            <span class="type-caption min-w-0 truncate text-body-muted">
              <span class="font-number">{{
                formatTime(transaction.requestedAt)
              }}</span>
              <span aria-hidden="true"> · </span>
              송금
              <span aria-hidden="true"> · </span>
              {{ transaction.bankName }} {{ transaction.accountNo }}
            </span>
            <span
              class="type-caption shrink-0 rounded-full px-sm py-xxs font-medium"
              :class="
                transaction.riskLevel
                  ? riskClass[transaction.riskLevel]
                  : 'bg-warning/10 text-warning'
              "
            >
              {{ getRiskLabel(transaction.riskLevel) }}
            </span>
          </span>
        </span>
      </button>
    </li>
  </ul>
</template>
