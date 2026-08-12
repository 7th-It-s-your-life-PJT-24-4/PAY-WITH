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

const resultCardDescription = computed(() => {
  if (transaction.value?.status === 'FAILED') {
    return '잔액 부족 등으로 인해 완료되지 않은 거래입니다.'
  }
  if (transaction.value?.status === 'BLOCKED') {
    return '이상거래 감지 시스템에 의해 안전하게 차단되었어요.'
  }
  if (transaction.value?.status === 'REJECTED') {
    return '보호자가 위험 거래로 판단하여 거절했어요.'
  }
  if (transaction.value?.status === 'CANCELED') {
    return '요청이 취소되어 실제 금액이 이동하지 않았어요.'
  }
  return undefined
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
    class="flex min-h-64 items-center justify-center text-[16px] text-body-muted"
    aria-live="polite"
  >
    거래 상세를 불러오는 중이에요.
  </section>

  <section
    v-else-if="transactionQuery.isError.value"
    class="flex min-h-64 flex-col items-center justify-center gap-md rounded-large border border-border bg-surface-card p-xl text-center shadow-card"
    role="alert"
  >
    <p class="text-[16px] font-medium text-body-muted">
      거래 상세를 불러오지 못했어요.
    </p>
    <Button label="목록으로 돌아가기" size="large" @click="goToHistory" />
  </section>

  <div v-else-if="transaction" class="flex flex-col gap-lg">
    <!-- 거래 핵심 요약 및 상세 카드 -->
    <TransactionSummaryCard :transaction="transaction" />

    <!-- 비정상/차단/실패 거래일 경우 결과 카드 노출 -->
    <TransactionBlockedCard
      v-if="shouldShowRejectedCard"
      :title="resultCardTitle"
      :description="resultCardDescription"
    />

    <!-- 위험 분석 카드 (충전이 아니고 위험 평가가 있는 경우) -->
    <TransactionRiskCard
      v-if="transaction.type !== 'CHARGE' && transaction.riskLevel"
      :transaction="transaction"
    />

    <!-- 하단 전체 너비 목록 버튼 -->
    <Button
      class="mt-sm w-full"
      label="목록으로 돌아가기"
      size="large"
      @click="goToHistory"
    />
  </div>
</template>
