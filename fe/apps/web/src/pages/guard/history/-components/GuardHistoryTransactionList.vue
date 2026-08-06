<script setup lang="ts">
import { useRouter } from 'vue-router'

import {
  guardTransactionCategoryIcons,
  guardTransactionStatusClasses,
  guardTransactionStatusLabels,
} from '@/pages/guard/-utils/guard-transaction-ui'
import type { GuardTransactionHistoryItem } from '@/schemas/transaction.schema'

const props = defineProps<{
  transactions: GuardTransactionHistoryItem[]
  wardId: number
  wardName: string
}>()

const router = useRouter()

const dateFormatter = new Intl.DateTimeFormat('ko-KR', {
  month: 'long',
  day: 'numeric',
})
const moneyFormatter = new Intl.NumberFormat('ko-KR')

const categoryByType = {
  CHARGE: 'charge',
  PAYMENT: 'payment',
  TRANSFER: 'transfer',
} as const

function getTransactionDate(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : dateFormatter.format(date)
}

function getTransactionAmount(transaction: GuardTransactionHistoryItem) {
  const prefix = transaction.type === 'CHARGE' ? '+' : '-'
  return `${prefix}${moneyFormatter.format(transaction.amount)}원`
}

function getTransactionDescription(transaction: GuardTransactionHistoryItem) {
  const name = transaction.counterpartyName ?? '거래 상대'
  if (transaction.type === 'CHARGE') return `${name} 충전`
  if (transaction.type === 'TRANSFER') return `${props.wardName} 계좌 → ${name}`
  return name
}

function getTransactionStatus(transaction: GuardTransactionHistoryItem) {
  if (transaction.riskLevel === 'DANGER') return 'danger'
  if (transaction.riskLevel === 'CAUTION') return 'warning'
  return 'safe'
}

function openTransaction(transactionId: number) {
  router.push({
    name: 'guard-transaction-detail',
    params: { id: transactionId },
    query: { wardId: props.wardId },
  })
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
          getTransactionDate(transactions[index - 1]!.createdAt) !==
            getTransactionDate(transaction.createdAt)
        "
        class="type-body-medium mb-xs text-gray-500"
        :class="index > 0 ? 'mt-md' : ''"
      >
        {{ getTransactionDate(transaction.createdAt) }}
      </p>

      <button
        class="flex h-[60px] w-full items-center bg-white px-sm text-left"
        type="button"
        :aria-label="`${getTransactionDate(transaction.createdAt)} ${getTransactionDescription(transaction)} 거래 상세 보기`"
        @click="openTransaction(transaction.transactionId)"
      >
        <span
          class="flex size-8 shrink-0 items-center justify-center rounded-[10px] bg-primary-500 text-white"
        >
          <component
            :is="
              guardTransactionCategoryIcons[categoryByType[transaction.type]]
            "
            class="size-[18px]"
            weight="fill"
            aria-hidden="true"
          />
        </span>
        <div class="ml-md min-w-0 flex-1">
          <p
            class="text-[14px] font-semibold leading-[1.2] tracking-[-0.28px] text-black"
          >
            {{ getTransactionAmount(transaction) }}
          </p>
          <p
            class="mt-xxs truncate text-[12px] font-medium leading-[1.2] tracking-[-0.24px] text-gray-700"
          >
            {{ getTransactionDescription(transaction) }}
          </p>
        </div>
        <span
          class="rounded-small px-[6px] py-xxs text-[10px] font-bold leading-[1.2] tracking-[-0.2px]"
          :class="
            guardTransactionStatusClasses[getTransactionStatus(transaction)]
          "
        >
          {{ guardTransactionStatusLabels[getTransactionStatus(transaction)] }}
        </span>
      </button>
    </template>
  </section>
</template>
