<script setup lang="ts">
import { useRouter } from 'vue-router'

import {
  guardTransactionCategoryIcons,
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

const riskBadgeClasses = {
  safe: 'bg-[#E6F7EE] text-[#1FA463]',
  warning: 'bg-[#FFF6DC] text-[#C08A00]',
  danger: 'bg-[#FFECEC] text-[#E14B4B]',
} as const

function getTransactionDate(value: string) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? value : dateFormatter.format(date)
}

function getTransactionAmount(transaction: GuardTransactionHistoryItem) {
  const prefix = transaction.type === 'CHARGE' ? '+' : '-'
  return `${prefix}${moneyFormatter.format(transaction.amount)}원`
}

function getAmountClass(transaction: GuardTransactionHistoryItem) {
  return transaction.type === 'CHARGE' ? 'text-[#8FBEF5]' : 'text-[#3D4348]'
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
        class="flex min-h-[74px] w-full items-center bg-white px-sm text-left"
        type="button"
        :aria-label="`${getTransactionDate(transaction.createdAt)} ${getTransactionDescription(transaction)} 거래 상세 보기`"
        @click="openTransaction(transaction.transactionId)"
      >
        <span
          class="flex size-11 shrink-0 items-center justify-center rounded-[14px] text-white"
          :class="
            transaction.type === 'CHARGE' ? 'bg-[#7ADCE3]' : 'bg-primary-500'
          "
        >
          <component
            :is="
              guardTransactionCategoryIcons[categoryByType[transaction.type]]
            "
            class="size-5"
            weight="fill"
            aria-hidden="true"
          />
        </span>
        <div class="ml-[13px] min-w-0 flex-1">
          <p
            class="truncate text-[14px] font-medium leading-[1.2] tracking-[-0.28px] text-gray-700"
          >
            {{ getTransactionDescription(transaction) }}
          </p>
          <p
            class="mt-xxs text-[17px] font-bold leading-[1.2] tracking-[-0.34px]"
            :class="getAmountClass(transaction)"
          >
            {{ getTransactionAmount(transaction) }}
          </p>
        </div>
        <span
          class="rounded-[8px] px-[11px] py-1 text-[12px] font-semibold leading-[1.2] tracking-[-0.24px]"
          :class="riskBadgeClasses[getTransactionStatus(transaction)]"
        >
          {{ guardTransactionStatusLabels[getTransactionStatus(transaction)] }}
        </span>
      </button>
    </template>
  </section>
</template>
