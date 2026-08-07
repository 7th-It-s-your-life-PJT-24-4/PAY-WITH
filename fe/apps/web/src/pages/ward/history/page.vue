<script setup lang="ts">
import { Search } from '@lucide/vue'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { wardTransactionHistoryOptions } from '@/lib/query/transaction-history'
import TransactionListItem from '@/pages/ward/history/-components/TransactionListItem.vue'
import { formatTransactionDate } from '@/pages/ward/history/-utils/transaction-format'
import type { TransactionHistoryType } from '@/schemas/transaction-history.schema'

type TransactionFilter = 'ALL' | TransactionHistoryType

const route = useRoute()
const router = useRouter()

const validCategories: TransactionFilter[] = [
  'ALL',
  'CHARGE',
  'TRANSFER',
  'PAYMENT',
]

const initialCategory = computed<TransactionFilter>(() => {
  const cat =
    typeof route.query.category === 'string'
      ? route.query.category.toUpperCase()
      : ''
  return validCategories.includes(cat as TransactionFilter)
    ? (cat as TransactionFilter)
    : 'ALL'
})
const initialKeyword = computed(() =>
  typeof route.query.keyword === 'string' ? route.query.keyword : '',
)
const initialPage = computed(() => {
  const p = Number(route.query.page)
  return Number.isInteger(p) && p >= 0 ? p : 0
})

const searchQuery = ref(initialKeyword.value)
const activeFilter = ref<TransactionFilter>(initialCategory.value)
const page = ref(initialPage.value)
const size = 20

const filters: Array<{ label: string; value: TransactionFilter }> = [
  { label: '전체', value: 'ALL' },
  { label: '충전', value: 'CHARGE' },
  { label: '송금', value: 'TRANSFER' },
  { label: '결제', value: 'PAYMENT' },
]

const queryParams = computed(() => ({
  category: activeFilter.value,
  keyword: searchQuery.value.trim().replaceAll(',', '') || undefined,
  page: page.value,
  size,
}))
const transactionQuery = useQuery(wardTransactionHistoryOptions(queryParams))
const transactions = computed(
  () => transactionQuery.data.value?.transactions ?? [],
)

function updateQueryParams() {
  const query = { ...route.query }

  if (activeFilter.value !== 'ALL') {
    query.category = activeFilter.value
  } else {
    delete query.category
  }

  const keyword = searchQuery.value.trim()
  if (keyword) {
    query.keyword = keyword
  } else {
    delete query.keyword
  }

  if (page.value > 0) {
    query.page = String(page.value)
  } else {
    delete query.page
  }

  void router.replace({ query })
}

watch(
  () => route.query,
  () => {
    if (activeFilter.value !== initialCategory.value) {
      activeFilter.value = initialCategory.value
    }
    if (searchQuery.value !== initialKeyword.value) {
      searchQuery.value = initialKeyword.value
    }
    if (page.value !== initialPage.value) {
      page.value = initialPage.value
    }
  },
)

watch([activeFilter, searchQuery], () => {
  page.value = 0
  updateQueryParams()
})

watch(page, () => {
  updateQueryParams()
})

const transactionGroups = computed(() => {
  const groups = new Map<string, typeof transactions.value>()

  for (const transaction of transactions.value) {
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

    <section
      v-if="transactionQuery.isPending.value"
      class="flex min-h-64 items-center justify-center text-body-muted"
      aria-live="polite"
    >
      거래 내역을 불러오는 중이에요.
    </section>

    <section
      v-else-if="transactionQuery.isError.value"
      class="flex min-h-64 flex-col items-center justify-center gap-md rounded-large border border-border bg-surface-card p-xl text-center shadow-card"
      role="alert"
    >
      <p class="type-body-medium text-body-muted">
        거래 내역을 불러오지 못했어요.
      </p>
      <button
        type="button"
        class="type-body-medium rounded-medium bg-primary-500 px-md py-sm text-on-action"
        @click="transactionQuery.refetch()"
      >
        다시 시도
      </button>
    </section>

    <div v-else-if="transactionGroups.length" class="grid gap-section">
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

      <nav
        class="flex items-center justify-center gap-md"
        aria-label="거래 내역 페이지"
      >
        <button
          type="button"
          class="type-body-medium rounded-medium border border-border-strong px-md py-sm disabled:opacity-40"
          :disabled="page === 0"
          @click="page -= 1"
        >
          이전
        </button>
        <span class="type-caption text-body-muted">{{ page + 1 }}페이지</span>
        <button
          type="button"
          class="type-body-medium rounded-medium border border-border-strong px-md py-sm disabled:opacity-40"
          :disabled="!transactionQuery.data.value?.hasNext"
          @click="page += 1"
        >
          다음
        </button>
      </nav>
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
