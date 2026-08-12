<script setup lang="ts">
import { ConfirmModal } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { HTTPError } from 'ky'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { guardHomeOptions } from '@/lib/query/guard/home'
import GuardAssetCard from '@/pages/guard/-components/GuardAssetCard.vue'
import GuardRiskTransactionSection from '@/pages/guard/-components/GuardRiskTransactionSection.vue'
import GuardSeniorAvatarList from '@/pages/guard/-components/GuardSeniorAvatarList.vue'
import GuardTransactionList from '@/pages/guard/-components/GuardTransactionList.vue'
import {
  parsePositiveRouteId,
  withGuardWardId,
} from '@/pages/guard/-utils/guard-route'
import { usePairingStore } from '@/stores/pairing.store'
import { useGuardStore } from '@/stores/guard.store'
import type { GuardRecentTransaction } from '@/schemas/guard-home.schema'
import type {
  GuardSeniorAvatar,
  GuardTransaction,
} from '@/mocks/guard-home.mock'

const route = useRoute()
const router = useRouter()
const pairingStore = usePairingStore()
const guardStore = useGuardStore()
const isPairingConfirmOpen = ref(false)
const routeWardId = computed(() => parsePositiveRouteId(route.query.wardId))
const selectedWardId = computed(
  () => routeWardId.value ?? guardStore.activeWardId,
)

const {
  data: guardHome,
  error: guardHomeError,
  isPending,
  isError,
} = useQuery(guardHomeOptions(selectedWardId))

const selectedWard = computed(() => guardHome.value?.selectedWard ?? null)
const seniors = computed<GuardSeniorAvatar[]>(() =>
  (guardHome.value?.wards ?? []).map(
    ({ wardId, name, avatarId, hasPending }) => ({
      id: String(wardId),
      name,
      imageUrl: `/images/avatar/avatar${avatarId}.png`,
      hasPending,
    }),
  ),
)
const activeSeniorId = computed(() =>
  selectedWard.value ? String(selectedWard.value.wardId) : '',
)
const recentGuardTransactions = computed(() =>
  (selectedWard.value?.recentTransactions ?? []).map(toGuardTransaction),
)
const pendingApprovals = computed(
  () => selectedWard.value?.pendingApprovals ?? [],
)
const formattedBalance = computed(() =>
  new Intl.NumberFormat('ko-KR').format(selectedWard.value?.balance ?? 0),
)

watch(guardHomeError, (error) => {
  if (
    !(error instanceof HTTPError) ||
    error.response.status !== 404 ||
    selectedWardId.value === null
  ) {
    return
  }

  guardStore.clearWardSelection()
  if (routeWardId.value !== null) {
    void router.replace({
      query: withGuardWardId(route.query, null),
    })
  }
})

watch(
  () => selectedWard.value?.wardId,
  (wardId) => {
    if (!wardId) return
    guardStore.selectWard(wardId)
    if (routeWardId.value !== wardId)
      void router.replace({
        query: withGuardWardId(route.query, wardId),
      })
  },
  { immediate: true },
)

function toGuardTransaction(
  transaction: GuardRecentTransaction,
): GuardTransaction {
  const category =
    transaction.type === 'CHARGE'
      ? 'charge'
      : transaction.type === 'TRANSFER'
        ? 'transfer'
        : 'payment'
  const status =
    transaction.riskLevel === 'DANGER'
      ? 'danger'
      : transaction.riskLevel === 'CAUTION'
        ? 'warning'
        : 'safe'
  const amountPrefix = transaction.type === 'CHARGE' ? '+' : '-'
  const counterpartyName = transaction.counterpartyName ?? '상대방'
  const description =
    transaction.type === 'CHARGE'
      ? `${counterpartyName} 충전`
      : transaction.type === 'TRANSFER'
        ? `${counterpartyName}님께 송금`
        : counterpartyName
  const occurredAt = new Date(transaction.createdAt)
  const date = Number.isNaN(occurredAt.getTime())
    ? ''
    : new Intl.DateTimeFormat('ko-KR', {
        month: 'long',
        day: 'numeric',
      }).format(occurredAt)

  return {
    id: String(transaction.transactionId),
    date,
    amount: `${amountPrefix}${new Intl.NumberFormat('ko-KR').format(transaction.amount)}원`,
    description,
    category,
    status,
  }
}

function selectSenior(wardId: string) {
  const parsedWardId = Number(wardId)
  if (!Number.isSafeInteger(parsedWardId) || parsedWardId <= 0) return
  guardStore.selectWard(parsedWardId)
  void router.replace({
    query: withGuardWardId(route.query, parsedWardId),
  })
}

function goToCharge() {
  if (!selectedWard.value) return

  router.push({
    name: 'guard-charge-be',
    query: withGuardWardId({}, selectedWard.value.wardId),
  })
}

function goToApprovalRequests() {
  router.push({
    name: 'guard-approval-requests',
    query: withGuardWardId({}, selectedWard.value?.wardId ?? null),
  })
}

async function startPairing() {
  const issued = await pairingStore.issueCode()
  if (!issued) return

  isPairingConfirmOpen.value = false
  router.push({
    name: 'guard-pairing-code',
    query: withGuardWardId({}, selectedWardId.value),
  })
}
</script>

<template>
  <main class="min-h-screen pb-[calc(66px+env(safe-area-inset-bottom))]">
    <section
      v-if="isPending"
      class="flex min-h-[calc(100dvh-66px-env(safe-area-inset-bottom))] items-center justify-center px-mobile-gutter text-center text-[16px] font-medium text-gray-500"
    >
      연결 정보를 불러오는 중이에요.
    </section>

    <section
      v-else-if="isError"
      class="flex min-h-[calc(100dvh-66px-env(safe-area-inset-bottom))] flex-col items-center justify-center px-mobile-gutter text-center"
    >
      <p class="text-[16px] font-medium text-gray-500">
        연결 정보를 불러오지 못했어요. 잠시 후 다시 시도해 주세요.
      </p>
    </section>

    <section
      v-else-if="seniors.length === 0"
      class="flex min-h-[calc(100dvh-66px-env(safe-area-inset-bottom))] flex-col items-center justify-center px-mobile-gutter text-center"
      aria-labelledby="guard-unpaired-title"
    >
      <h1
        id="guard-unpaired-title"
        class="text-[16px] font-medium leading-6 tracking-[-0.2px] text-gray-500"
      >
        연결된 시니어가 없어요.<br />
        시니어와 연동해 안전하게 보호하세요.
      </h1>
      <button
        class="mt-lg flex h-14 w-full items-center justify-center rounded-[10px] bg-primary-500 text-[16px] font-semibold leading-[1.2] tracking-[-0.32px] text-white shadow-[0_10px_15px_-3px_rgb(0_0_0/10%),0_4px_6px_-4px_rgb(0_0_0/10%)] transition-colors hover:bg-primary-400 active:bg-primary-300"
        type="button"
        @click="isPairingConfirmOpen = true"
      >
        시니어와 연결하기
      </button>
      <p
        v-if="pairingStore.errorMessage"
        class="mt-sm text-[14px] font-medium text-error"
        role="alert"
      >
        {{ pairingStore.errorMessage }}
      </p>
    </section>

    <div v-else class="px-mobile-gutter pt-md">
      <GuardSeniorAvatarList
        :seniors="seniors"
        :active-senior-id="activeSeniorId"
        @add="isPairingConfirmOpen = true"
        @select="selectSenior"
      />

      <GuardAssetCard
        class="mt-md"
        :senior-name="selectedWard?.name ?? ''"
        :balance="formattedBalance"
        @charge="goToCharge"
        @open-safe-accounts="
          router.push({
            name: 'guard-safe-account',
            query: { wardId: selectedWard?.wardId },
          })
        "
      />

      <GuardRiskTransactionSection
        class="mt-md"
        :pending-approvals="pendingApprovals"
        @more="goToApprovalRequests"
      />

      <GuardTransactionList
        :transactions="recentGuardTransactions"
        :ward-id="selectedWard?.wardId ?? null"
        @more="
          router.push({
            name: 'guard-history',
            query: withGuardWardId({}, selectedWard?.wardId ?? null),
          })
        "
      />
    </div>

    <ConfirmModal
      v-model:open="isPairingConfirmOpen"
      title="인증 코드를 생성할까요?"
      cancel-label="취소"
      :confirm-label="pairingStore.isIssuingCode ? '생성 중' : '생성'"
      :confirm-disabled="pairingStore.isIssuingCode"
      @confirm="startPairing"
    />
  </main>
</template>
