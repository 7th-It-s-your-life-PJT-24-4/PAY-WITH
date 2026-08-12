<script setup lang="ts">
import { PlusCircle, QrCode, ReceiptText, SendHorizontal } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { useQuery, useQueryClient } from '@tanstack/vue-query'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { getApiErrorCode, getApiErrorMessage } from '@/api/error'
import { wardHomeOptions } from '@/lib/query/ward/home'
import { wardWalletOptions } from '@/lib/query/ward/wallet'
import WardBalanceCard from '@/pages/ward/-components/WardBalanceCard.vue'
import WardPendingTransactionList from '@/pages/ward/-components/WardPendingTransactionList.vue'
import WardPendingTransactionSummaryCard from '@/pages/ward/-components/WardPendingTransactionSummaryCard.vue'
import WardPushNotificationPermissionCard from '@/pages/ward/-components/WardPushNotificationPermissionCard.vue'
import WardUnpairedHome from '@/pages/ward/-components/WardUnpairedHome.vue'
import { useChargeStore } from '@/stores/charge.store'
import { usePairingStore } from '@/stores/pairing.store'
import { useTransferStore } from '@/stores/transfer.store'
import type { PendingApprovalItem } from '@/schemas/home.schema'

const router = useRouter()
const pairingStore = usePairingStore()
const transferStore = useTransferStore()
const chargeStore = useChargeStore()
const queryClient = useQueryClient()

onMounted(() => {
  transferStore.reset()
  chargeStore.resetDraft()
})

const isWalletLocked = false
const homeQuery = useQuery(wardHomeOptions())
const requiresPairing = computed(
  () => getApiErrorCode(homeQuery.error.value) === 'WARD_001',
)
const balance = computed(() =>
  homeQuery.data.value?.wallet.balance.toLocaleString('ko-KR'),
)
const pendingTransactions = computed(
  () => homeQuery.data.value?.pendingApprovals ?? [],
)
const homeErrorMessage = ref('')

type PendingTransactionVariant = 'inline-list' | 'summary-card'

// 두 홈 화면 안을 비교하는 동안 이 값만 바꿔 승인 대기 거래 표시 방식을 전환합니다.
const pendingTransactionVariant = ref<PendingTransactionVariant>('summary-card')

const actions = [
  { label: '송금하기', value: 'transfer', icon: SendHorizontal },
  { label: '충전하기', value: 'charge', icon: PlusCircle },
  { label: '결제하기', value: 'payment', icon: QrCode },
  { label: '내역 조회', value: 'history', icon: ReceiptText },
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
  () => homeQuery.data.value,
  (home) => {
    if (home) pairingStore.markPaired()
  },
  { immediate: true },
)

watch(
  () => homeQuery.error.value,
  async (error) => {
    if (getApiErrorCode(error) === 'WARD_001') {
      pairingStore.reset()
      homeErrorMessage.value = ''
      return
    }
    homeErrorMessage.value = error
      ? await getApiErrorMessage(error, '홈 정보를 불러오지 못했습니다.')
      : ''
  },
  { immediate: true },
)
</script>

<template>
  <div
    v-if="homeQuery.isPending.value"
    class="flex flex-col gap-xl"
    aria-busy="true"
  >
    <div class="h-20 animate-pulse rounded-large bg-surface-card" />
    <div class="h-36 animate-pulse rounded-large bg-surface-card" />
  </div>

  <WardUnpairedHome
    v-else-if="requiresPairing"
    @connect="router.push({ name: 'ward-pairing' })"
  />

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

    <WardPushNotificationPermissionCard
      description="송금 결과와 보호자 처리 결과를 놓치지 않도록 알려드려요."
    />

    <WardBalanceCard :balance="balance ?? '0'" :locked="isWalletLocked" />

    <WardPendingTransactionList
      v-if="pendingTransactionVariant === 'inline-list'"
      :transactions="pendingTransactions"
      @select="openPendingTransaction"
    />
    <WardPendingTransactionSummaryCard
      v-else
      :count="pendingTransactions.length"
      @open="router.push({ name: 'ward-pending-transactions' })"
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
      >
        <template #leading>
          <component
            :is="action.icon"
            class="size-xl shrink-0"
            :stroke-width="2.25"
            aria-hidden="true"
          />
        </template>
      </Button>
    </section>
  </div>
</template>
