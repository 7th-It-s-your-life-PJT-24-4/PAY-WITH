<script setup lang="ts">
import { ArrowUpFromLine, ChevronRight, ShieldAlert } from '@lucide/vue'

import type { PendingApprovalItem } from '@/schemas/home.schema'

defineProps<{
  transactions: PendingApprovalItem[]
}>()

const emit = defineEmits<{
  select: [transaction: PendingApprovalItem]
}>()

function formatAmount(amount: number) {
  return `${amount.toLocaleString('ko-KR')}원`
}

function getAccessibleLabel(transaction: PendingApprovalItem) {
  return `송금 ${transaction.holderName} 님에게 ${formatAmount(transaction.amount)} 상세 확인`
}
</script>

<template>
  <section
    v-if="transactions.length"
    class="overflow-hidden rounded-large border border-warning/30 bg-surface-card shadow-card"
    aria-labelledby="pending-transaction-title"
  >
    <header class="flex items-start gap-md p-xl">
      <span
        class="flex size-12 shrink-0 items-center justify-center rounded-full bg-warning/15 text-warning"
        aria-hidden="true"
      >
        <ShieldAlert class="size-6" />
      </span>
      <div>
        <h2 id="pending-transaction-title" class="type-h3 text-body">
          보호자 승인을 기다리고 있어요
        </h2>
        <p class="type-body-medium mt-xs text-body-secondary">
          확인이 필요한 거래 {{ transactions.length }}건이 있습니다.
        </p>
      </div>
    </header>

    <ul class="divide-y divide-border border-t border-border">
      <li v-for="transaction in transactions" :key="transaction.approvalId">
        <button
          type="button"
          class="flex min-h-28 w-full items-center gap-md px-xl py-lg text-left transition-colors hover:bg-primary-900/50 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-primary-500"
          :aria-label="getAccessibleLabel(transaction)"
          @click="emit('select', transaction)"
        >
          <span
            class="flex size-11 shrink-0 items-center justify-center rounded-full bg-primary-900 text-primary-300"
            aria-hidden="true"
          >
            <ArrowUpFromLine class="size-6" />
          </span>
          <div class="min-w-0 flex-1">
            <span class="type-body-medium text-primary-300"> 송금 </span>
            <div class="mt-xxs flex items-baseline justify-between gap-md">
              <strong class="type-h3 truncate text-body">
                {{ transaction.holderName }} 님에게
              </strong>
              <strong class="type-h3 shrink-0 font-number text-body">
                {{ formatAmount(transaction.amount) }}
              </strong>
            </div>
            <p class="type-body-medium mt-xs truncate text-body-secondary">
              {{ transaction.bankName }} · {{ transaction.accountNo }}
            </p>
          </div>
          <ChevronRight
            class="size-6 shrink-0 text-body-muted"
            aria-hidden="true"
          />
        </button>
      </li>
    </ul>
  </section>
</template>
