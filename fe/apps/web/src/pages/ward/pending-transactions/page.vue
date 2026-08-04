<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { ShieldCheck } from '@lucide/vue'
import { useRouter } from 'vue-router'

import { getMockPendingTransactions } from '@/mocks/pending-transaction.mock'
import WardPendingTransactionList from '@/pages/ward/-components/WardPendingTransactionList.vue'
import { getPendingTransactionRoute } from '@/pages/ward/-utils/pending-transaction-route'
import type { PendingTransaction } from '@/types/pending-transaction'

const router = useRouter()
const pendingTransactions = getMockPendingTransactions()

function openPendingTransaction(transaction: PendingTransaction) {
  router.push(getPendingTransactionRoute(transaction))
}
</script>

<template>
  <div class="flex flex-col gap-xl">
    <WardPendingTransactionList
      :transactions="pendingTransactions"
      @select="openPendingTransaction"
    />

    <section
      v-if="!pendingTransactions.length"
      class="flex min-h-64 flex-col items-center justify-center rounded-large border border-border bg-surface-card p-xl text-center shadow-card"
      aria-live="polite"
    >
      <ShieldCheck class="size-12 text-primary-300" aria-hidden="true" />
      <h1 class="type-h3 mt-md text-body">승인 대기 중인 거래가 없습니다</h1>
      <p class="type-body-medium mt-xs text-body-muted">
        새로운 승인 대기 거래가 생기면 홈에서 알려드릴게요.
      </p>
      <Button
        class="mt-xl w-full"
        label="홈으로 돌아가기"
        variant="outline-primary"
        size="large"
        @click="router.replace({ name: 'ward-home' })"
      />
    </section>
  </div>
</template>
