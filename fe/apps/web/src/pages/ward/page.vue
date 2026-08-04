<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getApiErrorMessage } from '@/api/error'
import { wardHomeOptions } from '@/lib/query/ward/home'
import { wardWalletOptions } from '@/lib/query/ward/wallet'
import WardBalanceCard from '@/pages/ward/-components/WardBalanceCard.vue'
import WardPendingTransactionList from '@/pages/ward/-components/WardPendingTransactionList.vue'
import WardUnpairedHome from '@/pages/ward/-components/WardUnpairedHome.vue'
import { usePairingStore } from '@/stores/pairing.store'
import type { PendingApprovalItem } from '@/schemas/home.schema'

const router = useRouter()
const route = useRoute()
const pairingStore = usePairingStore()
const queryClient = useQueryClient()

if (route.query.pairing === 'unpaired') pairingStore.reset()

const isWalletLocked = false
const homeQuery = useQuery(
  wardHomeOptions(computed(() => pairingStore.isPaired)),
)
const balance = computed(() =>
  homeQuery.data.value?.wallet.balance.toLocaleString('ko-KR'),
)
const homeErrorMessage = ref('')

const actions = [
  { label: '송금하기', value: 'transfer' },
  { label: '충전하기', value: 'charge' },
  { label: '결제하기', value: 'payment' },
  { label: '내역 조회', value: 'history' },
]

function handleAction(value: string) {
  if (value === 'transfer') router.push({ name: 'ward-transfer' })
  if (value === 'charge') router.push({ name: 'ward-charge' })
  if (value === 'payment') router.push({ name: 'ward-payment' })
  if (value === 'history') router.push({ name: 'ward-transaction-history' })
}

function openPendingTransaction(transaction: PendingApprovalItem) {
  router.push({
    name: 'ward-approval-request-detail',
    params: { approvalId: transaction.approvalId },
  })
}

watch(
  () => homeQuery.data.value?.wallet,
  (wallet) => {
    if (wallet) queryClient.setQueryData(wardWalletOptions().queryKey, wallet)
  },
  { immediate: true },
)

watch(
  () => homeQuery.error.value,
  async (error) => {
    homeErrorMessage.value = error
      ? await getApiErrorMessage(error, '홈 정보를 불러오지 못했습니다.')
      : ''
  },
  { immediate: true },
)
</script>

<template>
  <WardUnpairedHome
    v-if="!pairingStore.isPaired"
    @connect="router.push({ name: 'ward-pairing' })"
  />

  <div
    v-else-if="homeQuery.isPending.value"
    class="flex flex-col gap-xl"
    aria-busy="true"
  >
    <div class="h-20 animate-pulse rounded-large bg-surface-card" />
    <div class="h-36 animate-pulse rounded-large bg-surface-card" />
  </div>

  <section
    v-else-if="homeQuery.isError.value"
    class="flex min-h-64 flex-col items-center justify-center rounded-large border border-border bg-surface-card p-xl text-center shadow-card"
    role="alert"
  >
    <h1 class="type-h3 text-body">홈 정보를 불러오지 못했습니다</h1>
    <p class="type-body-medium mt-xs text-body-muted">{{ homeErrorMessage }}</p>
    <Button
      class="mt-xl w-full"
      label="다시 시도"
      size="large"
      @click="homeQuery.refetch()"
    />
  </section>

  <div v-else-if="homeQuery.data.value" class="flex flex-col gap-xl">
    <section aria-labelledby="ward-welcome-title">
      <p class="type-body-medium text-body-secondary">환영합니다</p>
      <h1 id="ward-welcome-title" class="type-h1 mt-xs text-body">
        <span class="text-primary-500">{{ homeQuery.data.value.userName }}</span
        >님 안녕하세요
      </h1>
    </section>

    <WardBalanceCard :balance="balance ?? '0'" :locked="isWalletLocked" />

    <WardPendingTransactionList
      :transactions="homeQuery.data.value.pendingApprovals"
      @select="openPendingTransaction"
    />

    <section class="grid gap-md" aria-label="홈 주요 기능">
      <Button
        v-for="action in actions"
        :key="action.value"
        class="w-full"
        :label="action.label"
        variant="outline-primary"
        size="large"
        @click="handleAction(action.value)"
      />
    </section>
  </div>
</template>
