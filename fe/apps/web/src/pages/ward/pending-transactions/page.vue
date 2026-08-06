<script setup lang="ts">
import { ShieldCheck } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { getApiErrorMessage } from '@/api/error'
import { wardHomeOptions } from '@/lib/query/ward/home'
import WardPendingTransactionList from '@/pages/ward/-components/WardPendingTransactionList.vue'
import type { PendingApprovalItem } from '@/schemas/home.schema'

const router = useRouter()
const homeQuery = useQuery(wardHomeOptions())
const pendingTransactions = computed(
  () => homeQuery.data.value?.pendingApprovals ?? [],
)
const errorMessage = ref('')

function openPendingTransaction(transaction: PendingApprovalItem) {
  router.push({
    name: 'ward-approval-request-detail',
    params: { approvalId: transaction.approvalId },
  })
}

watch(
  () => homeQuery.error.value,
  async (error) => {
    errorMessage.value = error
      ? await getApiErrorMessage(error, '승인 대기 거래를 불러오지 못했습니다.')
      : ''
  },
  { immediate: true },
)
</script>

<template>
  <div
    v-if="homeQuery.isPending.value"
    class="flex min-h-64 items-center justify-center"
    aria-busy="true"
  >
    <p class="type-h3 text-body-muted">승인 대기 거래를 확인하고 있습니다</p>
  </div>

  <section
    v-else-if="homeQuery.isError.value"
    class="flex min-h-64 flex-col items-center justify-center rounded-large border border-border bg-surface-card p-xl text-center shadow-card"
    role="alert"
  >
    <h1 class="type-h3 text-body">승인 대기 거래를 불러오지 못했습니다</h1>
    <p class="type-body-medium mt-xs text-body-muted">{{ errorMessage }}</p>
    <div class="mt-xl grid w-full gap-md">
      <Button
        class="w-full"
        :label="
          homeQuery.isFetching.value ? '다시 확인하고 있습니다' : '다시 시도'
        "
        size="large"
        :disabled="homeQuery.isFetching.value"
        @click="homeQuery.refetch()"
      />
      <Button
        class="w-full"
        label="홈으로 돌아가기"
        variant="outline-primary"
        size="large"
        @click="router.replace({ name: 'ward-home' })"
      />
    </div>
  </section>

  <div v-else class="flex flex-col gap-xl">
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
