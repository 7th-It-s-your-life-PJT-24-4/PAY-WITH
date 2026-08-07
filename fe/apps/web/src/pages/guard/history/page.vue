<script setup lang="ts">
import { ConfirmModal } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { guardHomeOptions } from '@/lib/query/guard/home'
import { guardTransactionHistoryOptions } from '@/lib/query/guard/transaction'
import GuardSeniorAvatarList from '@/pages/guard/-components/GuardSeniorAvatarList.vue'
import {
  parsePositiveRouteId,
  withGuardWardId,
} from '@/pages/guard/-utils/guard-route'
import GuardHistoryTransactionList from '@/pages/guard/history/-components/GuardHistoryTransactionList.vue'
import type { GuardSeniorAvatar } from '@/mocks/guard-home.mock'
import type { TransactionRiskLevel } from '@/schemas/transaction.schema'
import { useGuardStore } from '@/stores/guard.store'
import { usePairingStore } from '@/stores/pairing.store'

type HistoryFilter = 'ALL' | TransactionRiskLevel

const filters: Array<{ label: string; value: HistoryFilter }> = [
  { label: '전체', value: 'ALL' },
  { label: '위험', value: 'DANGER' },
  { label: '주의', value: 'CAUTION' },
  { label: '안전', value: 'SAFE' },
]

const route = useRoute()
const router = useRouter()
const pairingStore = usePairingStore()
const guardStore = useGuardStore()
const routeWardId = computed(() => parsePositiveRouteId(route.query.wardId))
const selectedWardId = computed(
  () => routeWardId.value ?? guardStore.activeWardId,
)
const validRiskLevels: HistoryFilter[] = ['ALL', 'DANGER', 'CAUTION', 'SAFE']
const initialRiskLevel = computed<HistoryFilter>(() => {
  const level =
    typeof route.query.riskLevel === 'string'
      ? route.query.riskLevel.toUpperCase()
      : ''
  return validRiskLevels.includes(level as HistoryFilter)
    ? (level as HistoryFilter)
    : 'ALL'
})

const activeFilter = ref<HistoryFilter>(initialRiskLevel.value)
const isPairingConfirmOpen = ref(false)

watch(
  () => route.query.riskLevel,
  () => {
    if (activeFilter.value !== initialRiskLevel.value) {
      activeFilter.value = initialRiskLevel.value
    }
  },
)

function setFilter(filter: HistoryFilter) {
  activeFilter.value = filter
  const query = { ...route.query }
  if (filter !== 'ALL') {
    query.riskLevel = filter
  } else {
    delete query.riskLevel
  }
  void router.replace({ query })
}

const guardHomeQuery = useQuery(guardHomeOptions(selectedWardId))
const selectedWard = computed(
  () => guardHomeQuery.data.value?.selectedWard ?? null,
)
const seniors = computed<GuardSeniorAvatar[]>(() =>
  (guardHomeQuery.data.value?.wards ?? []).map(
    ({ wardId, name, avatarId }) => ({
      id: String(wardId),
      name,
      imageUrl: `/images/avatar/avatar${avatarId}.png`,
    }),
  ),
)
const activeWardId = computed(
  () => selectedWardId.value ?? selectedWard.value?.wardId ?? null,
)
const activeSeniorId = computed(() =>
  activeWardId.value === null ? '' : String(activeWardId.value),
)
const historyParams = computed(() => ({
  riskLevel: activeFilter.value === 'ALL' ? undefined : activeFilter.value,
  page: 0,
  size: 100,
}))
const transactionQuery = useQuery(
  guardTransactionHistoryOptions(activeWardId, historyParams),
)
const transactions = computed(
  () => transactionQuery.data.value?.transactions ?? [],
)

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

async function startPairing() {
  const issued = await pairingStore.issueCode()
  if (!issued) return

  isPairingConfirmOpen.value = false
  router.push({
    name: 'guard-pairing-code',
    query: withGuardWardId({}, activeWardId.value),
  })
}

function selectSenior(seniorId: string) {
  const wardId = Number(seniorId)
  if (!Number.isSafeInteger(wardId) || wardId <= 0) return
  guardStore.selectWard(wardId)
  void router.replace({
    query: withGuardWardId(route.query, wardId),
  })
}
</script>

<template>
  <main class="min-h-screen pb-[calc(66px+env(safe-area-inset-bottom))]">
    <section
      v-if="guardHomeQuery.isPending.value"
      class="flex min-h-[calc(100dvh-66px-env(safe-area-inset-bottom))] items-center justify-center px-mobile-gutter text-center text-[16px] font-medium text-gray-500"
      aria-busy="true"
    >
      연결 정보를 불러오는 중이에요.
    </section>

    <section
      v-else-if="guardHomeQuery.isError.value"
      class="flex min-h-[calc(100dvh-66px-env(safe-area-inset-bottom))] flex-col items-center justify-center px-mobile-gutter text-center"
      role="alert"
    >
      <p class="text-[16px] font-medium text-gray-500">
        연결 정보를 불러오지 못했어요.
      </p>
      <button
        class="mt-md min-h-11 px-md text-[16px] font-semibold text-primary-500"
        type="button"
        @click="guardHomeQuery.refetch()"
      >
        다시 시도
      </button>
    </section>

    <section
      v-else-if="seniors.length === 0"
      class="flex min-h-[calc(100dvh-66px-env(safe-area-inset-bottom))] flex-col items-center justify-center px-mobile-gutter text-center"
    >
      <p class="text-[16px] font-medium leading-6 text-gray-500">
        연결된 시니어가 없어요.<br />시니어를 연결한 뒤 거래 내역을 확인해
        주세요.
      </p>
      <button
        class="mt-lg h-14 w-full rounded-[8px] bg-primary-500 text-[16px] font-semibold text-white"
        type="button"
        @click="isPairingConfirmOpen = true"
      >
        시니어와 연결하기
      </button>
    </section>

    <div v-else class="px-mobile-gutter pt-md">
      <GuardSeniorAvatarList
        :seniors="seniors"
        :active-senior-id="activeSeniorId"
        @add="isPairingConfirmOpen = true"
        @select="selectSenior"
      />

      <div class="mt-md flex gap-xs" aria-label="거래 위험도 필터">
        <button
          v-for="filter in filters"
          :key="filter.value"
          class="flex h-8 min-w-14 items-center justify-center rounded-[8px] border-[1.5px] border-primary-500 px-[15px] text-[14px] font-semibold leading-[1.6] tracking-[-0.28px] transition-colors duration-300"
          :class="
            activeFilter === filter.value
              ? 'bg-primary-500 text-white'
              : 'bg-white text-primary-500'
          "
          type="button"
          :aria-pressed="activeFilter === filter.value"
          @click="setFilter(filter.value)"
        >
          {{ filter.label }}
        </button>
      </div>

      <GuardHistoryTransactionList
        v-if="activeWardId !== null && transactions.length > 0"
        class="mt-md pb-md"
        :transactions="transactions"
        :ward-id="activeWardId"
        :ward-name="selectedWard?.name ?? ''"
      />

      <section
        v-else-if="transactionQuery.isPending.value"
        class="flex min-h-[360px] items-center justify-center text-center text-[16px] font-medium text-gray-500"
        aria-busy="true"
      >
        거래 내역을 불러오는 중이에요.
      </section>

      <section
        v-else-if="transactionQuery.isError.value"
        class="flex min-h-[360px] flex-col items-center justify-center text-center"
        role="alert"
      >
        <p class="text-[16px] font-medium text-gray-500">
          거래 내역을 불러오지 못했어요.
        </p>
        <button
          class="mt-md min-h-11 px-md text-[16px] font-semibold text-primary-500"
          type="button"
          @click="transactionQuery.refetch()"
        >
          다시 시도
        </button>
      </section>

      <p
        v-else
        class="flex min-h-[360px] items-center justify-center text-center text-[16px] font-medium leading-6 text-gray-500"
      >
        {{
          activeFilter === 'ALL'
            ? '아직 거래 내역이 없어요.'
            : '해당 위험도의 거래 내역이 없어요.'
        }}
      </p>
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
