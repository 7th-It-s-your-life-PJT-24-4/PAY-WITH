<script setup lang="ts">
import { Search } from '@lucide/vue'
import { computed, ref } from 'vue'

import { mockWardTransactions } from '@/mocks/transaction.mock'
import TransactionListItem from '@/pages/ward/history/-components/TransactionListItem.vue'
import { formatTransactionDate } from '@/pages/ward/history/-utils/transaction-format'

type TransactionFilter = 'ALL' | 'CREDIT' | 'DEBIT' | 'PAYMENT'

const searchQuery = ref('')
const activeFilter = ref<TransactionFilter>('ALL')

const filters: Array<{ label: string; value: TransactionFilter }> = [
  { label: '전체', value: 'ALL' },
  { label: '받은 돈', value: 'CREDIT' },
  { label: '보낸 돈', value: 'DEBIT' },
  { label: '결제', value: 'PAYMENT' },
]

const filteredTransactions = computed(() => {
  const query = searchQuery.value.trim().replaceAll(',', '').toLowerCase()

  return mockWardTransactions.filter((transaction) => {
    const matchesFilter =
      activeFilter.value === 'ALL' ||
      (activeFilter.value === 'PAYMENT' && transaction.type === 'PAYMENT') ||
      (activeFilter.value !== 'PAYMENT' &&
        transaction.type === 'TRANSFER' &&
        transaction.direction === activeFilter.value)

    if (!matchesFilter) return false
    if (!query) return true

    const searchableText = [
      transaction.title,
      transaction.amount.toString(),
      transaction.memo,
      transaction.methodLabel,
      transaction.transfer?.bankName,
      transaction.transfer?.accountNo,
    ]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()

    return searchableText.includes(query)
  })
})

const transactionGroups = computed(() => {
  const groups = new Map<string, typeof mockWardTransactions>()

  for (const transaction of filteredTransactions.value) {
    const date = transaction.occurredAt.slice(0, 10)
    const group = groups.get(date) ?? []
    group.push(transaction)
    groups.set(date, group)
  }

  return [...groups.entries()].map(([date, transactions]) => ({
    date,
    label: formatTransactionDate(`${date}T00:00:00+09:00`),
    transactions,
  }))
})
</script>

<template>
  <div class="flex flex-col gap-xl">
    <section class="grid gap-md" aria-label="거래 내역 검색과 필터">
      <label class="relative block">
        <span class="sr-only">거래 내역 검색</span>
        <Search
          class="pointer-events-none absolute top-1/2 left-md size-6 -translate-y-1/2 text-body-muted"
          aria-hidden="true"
        />
        <input
          v-model="searchQuery"
          type="search"
          class="type-body-medium h-[56px] w-full rounded-medium border border-border bg-surface-card pr-md pl-14 text-body shadow-card outline-none placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20"
          placeholder="상점명, 받는 분 또는 금액 검색"
        />
      </label>

      <div class="grid grid-cols-2 gap-sm" role="group" aria-label="거래 종류">
        <button
          v-for="filter in filters"
          :key="filter.value"
          type="button"
          class="type-h4 min-h-touch-target rounded-full border px-md transition-colors focus-visible:outline-2 focus-visible:outline-focus"
          :class="
            activeFilter === filter.value
              ? 'border-primary-500 bg-primary-500 text-on-action'
              : 'border-border-strong bg-surface-card text-body'
          "
          :aria-pressed="activeFilter === filter.value"
          @click="activeFilter = filter.value"
        >
          {{ filter.label }}
        </button>
      </div>
    </section>

    <div v-if="transactionGroups.length" class="grid gap-section">
      <section
        v-for="group in transactionGroups"
        :key="group.date"
        :aria-labelledby="`transaction-date-${group.date}`"
      >
        <h2
          :id="`transaction-date-${group.date}`"
          class="type-h3 mb-md text-body-secondary"
        >
          {{ group.label }}
        </h2>
        <div class="grid gap-md">
          <TransactionListItem
            v-for="transaction in group.transactions"
            :key="transaction.transactionId"
            :transaction="transaction"
          />
        </div>
      </section>
    </div>

    <section
      v-else
      class="flex min-h-64 flex-col items-center justify-center rounded-large border border-border bg-surface-card p-xl text-center shadow-card"
      aria-live="polite"
    >
      <Search class="size-12 text-body-muted" aria-hidden="true" />
      <h2 class="type-h3 mt-md text-body">찾은 거래가 없습니다</h2>
      <p class="type-body-medium mt-xs text-body-muted">
        검색어나 거래 종류를 다시 확인해 주세요.
      </p>
    </section>
  </div>
</template>
