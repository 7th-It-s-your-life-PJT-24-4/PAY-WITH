<script setup lang="ts">
import { ArrowDownToLine, ArrowUpFromLine, ReceiptText } from '@lucide/vue'
import { computed } from 'vue'

import {
  formatTransactionAmount,
  formatTransactionDateTime,
  getTransactionTypeLabel,
} from '@/pages/ward/history/-utils/transaction-format'
import type { WardTransaction } from '@/types/transaction'

const props = defineProps<{
  transaction: WardTransaction
}>()

const icon = computed(() => {
  if (props.transaction.type === 'PAYMENT') return ReceiptText
  return props.transaction.direction === 'CREDIT'
    ? ArrowDownToLine
    : ArrowUpFromLine
})

const counterpartyLabel = computed(() =>
  props.transaction.type === 'PAYMENT' ? '가맹점' : '거래한 분',
)
</script>

<template>
  <section
    class="rounded-large border border-border bg-surface-card p-xl shadow-card"
    aria-labelledby="transaction-summary-title"
  >
    <div class="flex items-center gap-md">
      <span
        class="flex size-12 items-center justify-center rounded-full bg-primary-900 text-primary-300"
        aria-hidden="true"
      >
        <component :is="icon" class="size-6" />
      </span>
      <div>
        <p class="type-body-medium text-body-muted">거래 종류</p>
        <h2 id="transaction-summary-title" class="type-h3 text-body">
          {{ getTransactionTypeLabel(transaction.type, transaction.direction) }}
        </h2>
      </div>
    </div>

    <dl class="mt-lg grid grid-cols-2 gap-x-lg gap-y-lg">
      <div class="col-span-2 border-b border-border pb-lg">
        <dt class="type-body-medium text-body-muted">
          {{ counterpartyLabel }}
        </dt>
        <dd class="type-h3 mt-xs text-body">{{ transaction.title }}</dd>
        <template v-if="transaction.transfer">
          <dd class="type-body-medium mt-xs text-body-secondary">
            {{ transaction.transfer.bankName }}
            <span class="font-number">{{
              transaction.transfer.accountNumber
            }}</span>
          </dd>
        </template>
        <template v-if="transaction.memo">
          <dt class="type-body-medium mt-md text-body-muted">메모</dt>
          <dd class="type-h3 mt-xs text-body">{{ transaction.memo }}</dd>
        </template>
      </div>

      <div class="col-span-2 flex items-center justify-between gap-md">
        <dt class="type-body-medium text-body-muted">거래 금액</dt>
        <dd
          class="type-h2 font-number"
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
        </dd>
      </div>

      <div>
        <dt class="type-body-medium text-body-muted">거래 일시</dt>
        <dd class="type-body-medium mt-xs font-number text-body">
          {{ formatTransactionDateTime(transaction.occurredAt) }}
        </dd>
      </div>
      <div>
        <dt class="type-body-medium text-body-muted">거래 수단</dt>
        <dd class="type-body-medium mt-xs text-body">
          {{ transaction.methodLabel }}
        </dd>
      </div>
    </dl>
  </section>
</template>
