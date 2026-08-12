<script setup lang="ts">
import {
  ArrowDownToLine,
  ArrowUpFromLine,
  Landmark,
  ReceiptText,
} from '@lucide/vue'
import { computed } from 'vue'

import {
  formatTransactionAmount,
  formatTransactionBalance,
  formatTransactionDateTime,
  getTransactionTypeLabel,
} from '@/pages/ward/history/-utils/transaction-format'
import type { WardTransaction } from '@/types/transaction'

const props = defineProps<{
  transaction: WardTransaction
}>()

const isInterrupted = computed(
  () =>
    props.transaction.status === 'BLOCKED' ||
    props.transaction.status === 'REJECTED' ||
    props.transaction.status === 'CANCELED' ||
    props.transaction.status === 'FAILED',
)

const icon = computed(() => {
  if (props.transaction.type === 'CHARGE') return Landmark
  if (props.transaction.type === 'PAYMENT') return ReceiptText
  return props.transaction.direction === 'CREDIT' ||
    props.transaction.direction === 'IN'
    ? ArrowDownToLine
    : ArrowUpFromLine
})

const counterpartyLabel = computed(() =>
  props.transaction.type === 'PAYMENT'
    ? '사용처'
    : props.transaction.type === 'CHARGE'
      ? '충전 계좌'
      : '받는 분',
)

const amountColorClass = computed(() => {
  if (isInterrupted.value) {
    return 'text-body-muted line-through decoration-2'
  }
  if (props.transaction.riskLevel === 'DANGER') {
    return 'text-error'
  }
  if (
    props.transaction.direction === 'CREDIT' ||
    props.transaction.direction === 'IN'
  ) {
    return 'text-primary-300'
  }
  return 'text-body'
})
</script>

<template>
  <section
    class="overflow-hidden rounded-large border border-border bg-surface-card p-xl shadow-card"
    aria-labelledby="transaction-summary-title"
  >
    <!-- 상단 Hero 영역: 아이콘 + 거래 대상 + 대형 금액 -->
    <div class="flex flex-col items-center text-center">
      <span
        class="flex size-14 items-center justify-center rounded-full bg-primary-900 text-primary-300"
        aria-hidden="true"
      >
        <component :is="icon" class="size-7" />
      </span>

      <h2
        id="transaction-summary-title"
        class="mt-md text-[24px] font-bold leading-tight tracking-[-0.48px] text-body"
      >
        {{ transaction.title }}
      </h2>

      <p
        v-if="transaction.transfer"
        class="mt-xs text-[16px] font-medium text-body-secondary"
      >
        {{ transaction.transfer.bankName }}
        <span class="font-number">{{ transaction.transfer.accountNo }}</span>
      </p>

      <p
        class="type-amount mt-md font-number font-bold tracking-tight"
        :class="amountColorClass"
      >
        {{ formatTransactionAmount(transaction.amount, transaction.direction) }}
      </p>
    </div>

    <!-- 1열 상세 내역 (Single Column Key-Value List) -->
    <dl
      class="mt-xl grid min-w-0 grid-cols-[minmax(0,1fr)] gap-y-sm border-t border-border pt-lg"
    >
      <!-- 거래 구분 -->
      <div
        class="flex min-h-12 items-center justify-between gap-md border-b border-border/60 py-xs"
      >
        <dt class="shrink-0 text-[16px] font-medium text-body-muted">
          거래 종류
        </dt>
        <dd
          class="min-w-0 flex-1 truncate text-right text-[18px] font-semibold text-body"
        >
          {{ getTransactionTypeLabel(transaction.type, transaction.direction) }}
        </dd>
      </div>

      <!-- 거래 상대방 라벨 및 내용 (수취인/가맹점) -->
      <div
        class="flex min-h-12 items-center justify-between gap-md border-b border-border/60 py-xs"
      >
        <dt class="shrink-0 text-[16px] font-medium text-body-muted">
          {{ counterpartyLabel }}
        </dt>
        <dd
          class="min-w-0 flex-1 truncate text-right text-[18px] font-semibold text-body"
        >
          {{ transaction.title }}
        </dd>
      </div>

      <!-- 거래 일시 -->
      <div
        class="flex min-h-12 items-center justify-between gap-md border-b border-border/60 py-xs"
      >
        <dt class="shrink-0 text-[16px] font-medium text-body-muted">
          거래 일시
        </dt>
        <dd
          class="min-w-0 flex-1 truncate text-right font-number text-[18px] font-semibold text-body"
        >
          {{ formatTransactionDateTime(transaction.occurredAt) }}
        </dd>
      </div>

      <!-- 거래 수단 -->
      <div
        class="flex min-h-12 items-center justify-between gap-md border-b border-border/60 py-xs"
      >
        <dt class="shrink-0 text-[16px] font-medium text-body-muted">
          거래 수단
        </dt>
        <dd
          class="min-w-0 flex-1 truncate text-right text-[18px] font-semibold text-body"
        >
          {{ transaction.methodLabel }}
        </dd>
      </div>

      <!-- 메모 (존재할 경우) -->
      <div
        v-if="transaction.memo"
        class="flex min-h-12 items-center justify-between gap-md border-b border-border/60 py-xs"
      >
        <dt class="shrink-0 text-[16px] font-medium text-body-muted">메모</dt>
        <dd
          class="min-w-0 flex-1 truncate text-right text-[18px] font-semibold text-body"
        >
          {{ transaction.memo }}
        </dd>
      </div>

      <!-- 거래 후 잔액 -->
      <div class="flex min-h-12 items-center justify-between gap-md pt-sm">
        <dt class="shrink-0 text-[16px] font-medium text-body-muted">
          거래 후 잔액
        </dt>
        <dd
          class="min-w-0 flex-1 truncate text-right font-number text-[20px] font-bold text-primary-300"
        >
          {{
            transaction.balanceAfter == null
              ? '-'
              : formatTransactionBalance(transaction.balanceAfter)
          }}
        </dd>
      </div>
    </dl>
  </section>
</template>
