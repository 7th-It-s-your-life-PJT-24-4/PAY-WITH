<script setup lang="ts">
import { useRouter } from 'vue-router'

import GuardTransactionCard from '@/pages/guard/-components/GuardTransactionCard.vue'
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

      <GuardTransactionCard
        :amount="getTransactionAmount(transaction)"
        :accessible-label="`${getTransactionDate(transaction.createdAt)} ${getTransactionDescription(transaction)} 거래 상세 보기`"
        :category="categoryByType[transaction.type]"
        :description="getTransactionDescription(transaction)"
        :status="getTransactionStatus(transaction)"
        @select="openTransaction(transaction.transactionId)"
      />
    </template>
  </section>
</template>
