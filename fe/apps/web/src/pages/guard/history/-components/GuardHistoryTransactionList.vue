<script setup lang="ts">
import { PhBank, PhPaperPlaneTilt, PhShoppingBag } from '@phosphor-icons/vue'

import type { GuardTransactionHistoryItem } from '@/schemas/transaction-history.schema'

defineProps<{
  transactions: GuardTransactionHistoryItem[]
}>()

const typeIcons = {
  CHARGE: PhBank,
  TRANSFER: PhPaperPlaneTilt,
  PAYMENT: PhShoppingBag,
}

const riskClasses = {
  SAFE: 'bg-success/10 text-success',
  CAUTION: 'bg-warning/10 text-warning',
  DANGER: 'bg-error/10 text-error',
}

const riskLabels = {
  SAFE: '안전',
  CAUTION: '주의',
  DANGER: '위험',
}

function formatDate(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value.slice(0, 10)
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'long',
    day: 'numeric',
  }).format(date)
}

function formatAmount(transaction: GuardTransactionHistoryItem) {
  const sign = transaction.type === 'CHARGE' ? '+' : '-'
  return `${sign}${transaction.amount.toLocaleString('ko-KR')}원`
}
</script>

<template>
  <section aria-label="거래 내역">
    <template
      v-for="(transaction, index) in transactions"
      :key="transaction.transactionId"
    >
      <p
        v-if="
          index === 0 ||
          formatDate(transactions[index - 1]?.createdAt ?? '') !==
            formatDate(transaction.createdAt)
        "
        class="type-body-medium mb-xs text-gray-500"
        :class="index > 0 ? 'mt-md' : ''"
      >
        {{ formatDate(transaction.createdAt) }}
      </p>

      <article
        class="flex h-[60px] w-full items-center bg-white px-sm text-left"
        :aria-label="`${formatDate(transaction.createdAt)} ${transaction.counterpartyName ?? '거래'} ${formatAmount(transaction)}`"
      >
        <span
          class="flex size-8 shrink-0 items-center justify-center rounded-[10px] bg-primary-500 text-white"
        >
          <component
            :is="typeIcons[transaction.type]"
            class="size-[18px]"
            weight="fill"
            aria-hidden="true"
          />
        </span>
        <div class="ml-md min-w-0 flex-1">
          <p
            class="text-[14px] font-semibold leading-[1.2] tracking-[-0.28px] text-black"
          >
            {{ formatAmount(transaction) }}
          </p>
          <p
            class="mt-xxs truncate text-[12px] font-medium leading-[1.2] tracking-[-0.24px] text-gray-700"
          >
            {{ transaction.counterpartyName ?? '거래 상대 정보 없음' }}
          </p>
        </div>
        <span
          v-if="transaction.riskLevel"
          class="rounded-small px-[6px] py-xxs text-[10px] font-bold leading-[1.2] tracking-[-0.2px]"
          :class="riskClasses[transaction.riskLevel]"
        >
          {{ riskLabels[transaction.riskLevel] }}
        </span>
      </article>
    </template>
  </section>
</template>
