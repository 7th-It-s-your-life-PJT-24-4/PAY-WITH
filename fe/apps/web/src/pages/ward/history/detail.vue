<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getMockWardTransaction } from '@/mocks/transaction.mock'
import TransactionRiskCard from '@/pages/ward/history/-components/TransactionRiskCard.vue'
import TransactionSummaryCard from '@/pages/ward/history/-components/TransactionSummaryCard.vue'

const route = useRoute()
const router = useRouter()

const transaction = computed(() =>
  getMockWardTransaction(Number(route.params.transactionId)),
)

function goToHistory() {
  router.replace({ name: 'ward-transaction-history' })
}
</script>

<template>
  <div v-if="transaction" class="flex flex-col gap-xl">
    <TransactionSummaryCard :transaction="transaction" />
    <TransactionRiskCard
      v-if="transaction.direction !== 'CREDIT'"
      :transaction="transaction"
    />

    <Button
      class="w-full"
      label="목록으로 돌아가기"
      size="large"
      @click="goToHistory"
    />
  </div>
</template>
