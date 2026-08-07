<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { wardTransactionDetailOptions } from '@/lib/query/transaction-history'
import TransactionBlockedCard from '@/pages/ward/history/-components/TransactionBlockedCard.vue'
import TransactionRiskCard from '@/pages/ward/history/-components/TransactionRiskCard.vue'
import TransactionSummaryCard from '@/pages/ward/history/-components/TransactionSummaryCard.vue'

const route = useRoute()
const router = useRouter()

const transactionId = computed(() => Number(route.params.transactionId))
const transactionQuery = useQuery(wardTransactionDetailOptions(transactionId))
const transaction = computed(() => transactionQuery.data.value)
const shouldShowRejectedCard = computed(
  () =>
    transaction.value?.status === 'BLOCKED' ||
    transaction.value?.status === 'REJECTED' ||
    transaction.value?.status === 'CANCELED' ||
    transaction.value?.status === 'FAILED',
)
const resultCardTitle = computed(() => {
  if (transaction.value?.status === 'CANCELED') return '취소된 거래입니다.'
  if (transaction.value?.status === 'FAILED') return '실패한 거래입니다.'
  if (transaction.value?.status === 'BLOCKED')
    return '시스템이 차단한 거래입니다.'
  if (transaction.value?.status === 'REJECTED')
    return '보호자가 거절한 거래입니다.'
  return '거절된 거래입니다.'
})

function goToHistory() {
  if (window.history.length > 1) {
    router.back()
  } else {
    void router.replace({ name: 'ward-transaction-history' })
  }
}
</script>

<template>
  <section
    v-if="transactionQuery.isPending.value"
    class="flex min-h-64 items-center justify-center text-body-muted"
    aria-live="polite"
  >
    거래 상세를 불러오는 중이에요.
  </section>

  <section
    v-else-if="transactionQuery.isError.value"
    class="flex min-h-64 flex-col items-center justify-center gap-md rounded-large border border-border bg-surface-card p-xl text-center shadow-card"
    role="alert"
  >
    <p class="type-body-medium text-body-muted">
      거래 상세를 불러오지 못했어요.
    </p>
    <Button label="목록으로 돌아가기" @click="goToHistory" />
  </section>

  <div v-else-if="transaction" class="flex flex-col gap-xl">
    <TransactionSummaryCard :transaction="transaction" />
    <TransactionRiskCard
      v-if="transaction.type !== 'CHARGE' && transaction.riskLevel"
      :transaction="transaction"
    />
    <TransactionBlockedCard
      v-if="shouldShowRejectedCard"
      :title="resultCardTitle"
    />

    <Button
      class="w-full"
      label="목록으로 돌아가기"
      size="large"
      @click="goToHistory"
    />
  </div>
</template>
