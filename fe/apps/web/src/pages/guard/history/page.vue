<script setup lang="ts">
import { PhCaretDown } from '@phosphor-icons/vue'
import { BottomSheet, ConfirmModal } from '@pay-with/ui'
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

type TransactionTypeFilter = 'ALL' | 'CHARGE' | 'PAYMENT' | 'TRANSFER'
type RiskFilter = 'ALL' | TransactionRiskLevel

const typeFilters: Array<{ label: string; value: TransactionTypeFilter }> = [
  { label: '전체', value: 'ALL' },
  { label: '충전', value: 'CHARGE' },
  { label: '결제', value: 'PAYMENT' },
  { label: '송금', value: 'TRANSFER' },
]
const riskFilters: Array<{ label: string; value: RiskFilter }> = [
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
const validTransactionTypes: TransactionTypeFilter[] = [
  'ALL',
  'CHARGE',
  'PAYMENT',
  'TRANSFER',
]
const validRiskLevels: RiskFilter[] = ['ALL', 'DANGER', 'CAUTION', 'SAFE']
const initialTransactionType = computed<TransactionTypeFilter>(() => {
  const type =
    typeof route.query.type === 'string' ? route.query.type.toUpperCase() : ''
  return validTransactionTypes.includes(type as TransactionTypeFilter)
    ? (type as TransactionTypeFilter)
    : 'ALL'
})
const initialRiskLevel = computed<RiskFilter>(() => {
  const level =
    typeof route.query.riskLevel === 'string'
      ? route.query.riskLevel.toUpperCase()
      : ''
  return validRiskLevels.includes(level as RiskFilter)
    ? (level as RiskFilter)
    : 'ALL'
})

const activeTransactionType = ref<TransactionTypeFilter>(
  initialTransactionType.value,
)
const activeRiskFilter = ref<RiskFilter>(initialRiskLevel.value)
const isRiskFilterOpen = ref(false)
const isPairingConfirmOpen = ref(false)
const activeRiskLabel = computed(
  () =>
    riskFilters.find((filter) => filter.value === activeRiskFilter.value)
      ?.label ?? '전체',
)

watch(
  () => [route.query.type, route.query.riskLevel],
  () => {
    if (activeTransactionType.value !== initialTransactionType.value) {
      activeTransactionType.value = initialTransactionType.value
    }
    if (activeRiskFilter.value !== initialRiskLevel.value) {
      activeRiskFilter.value = initialRiskLevel.value
    }
  },
)

function updateFilterQuery() {
  const query = { ...route.query }

  if (activeTransactionType.value !== 'ALL') {
    query.type = activeTransactionType.value
  } else {
    delete query.type
  }

  if (activeRiskFilter.value !== 'ALL') {
    query.riskLevel = activeRiskFilter.value
  } else {
    delete query.riskLevel
  }

  void router.replace({ query })
}

function setTransactionType(filter: TransactionTypeFilter) {
  activeTransactionType.value = filter
  updateFilterQuery()
}

function setRiskFilter(filter: RiskFilter) {
  activeRiskFilter.value = filter
  isRiskFilterOpen.value = false
  updateFilterQuery()
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
  type:
    activeTransactionType.value === 'ALL'
      ? undefined
      : activeTransactionType.value,
  riskLevel:
    activeRiskFilter.value === 'ALL' ? undefined : activeRiskFilter.value,
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

      <div class="mt-md flex gap-xs" role="group" aria-label="거래 종류 필터">
        <button
          v-for="filter in typeFilters"
          :key="filter.value"
          class="flex h-8 min-w-14 items-center justify-center rounded-[8px] border-[1.5px] border-primary-500 px-[15px] text-[14px] font-semibold leading-[1.6] tracking-[-0.28px] transition-colors duration-300"
          :class="
            activeTransactionType === filter.value
              ? 'bg-primary-500 text-white'
              : 'bg-white text-primary-500'
          "
          type="button"
          :aria-pressed="activeTransactionType === filter.value"
          @click="setTransactionType(filter.value)"
        >
          {{ filter.label }}
        </button>
      </div>

      <section
        class="-mx-mobile-gutter mt-xs flex min-h-12 items-center justify-between border-b border-gray-900 px-mobile-gutter"
        aria-label="거래 내역 필터"
      >
        <button
          class="flex min-h-11 items-center gap-[5px] text-[14px] font-semibold text-gray-600 outline-none focus-visible:ring-2 focus-visible:ring-primary-500"
          type="button"
          aria-haspopup="dialog"
          :aria-expanded="isRiskFilterOpen"
          :aria-label="`위험도 필터: ${activeRiskLabel}`"
          @click="isRiskFilterOpen = true"
        >
          {{ activeRiskLabel }}
          <PhCaretDown class="size-4 text-gray-500" aria-hidden="true" />
        </button>
        <span class="text-[13px] font-medium text-gray-500">
          총 {{ transactions.length }}건
        </span>
      </section>

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
          activeTransactionType === 'ALL' && activeRiskFilter === 'ALL'
            ? '아직 거래 내역이 없어요.'
            : '해당 조건의 거래 내역이 없어요.'
        }}
      </p>
    </div>

    <BottomSheet
      v-model:open="isRiskFilterOpen"
      title="내역 선택"
      description="확인할 거래의 위험도를 선택해 주세요."
      content-class="pb-[calc(28px+env(safe-area-inset-bottom))]"
    >
      <fieldset class="mt-sm">
        <legend class="sr-only">거래 위험도</legend>
        <label
          v-for="filter in riskFilters"
          :key="filter.value"
          class="flex min-h-[52px] cursor-pointer items-center gap-sm px-xxs text-[16px] text-gray-600"
          :class="
            activeRiskFilter === filter.value
              ? 'font-bold text-black'
              : 'font-medium'
          "
        >
          <input
            class="size-[22px] shrink-0 cursor-pointer appearance-none rounded-full border-2 border-gray-300 bg-white outline-none transition-[border] checked:border-[7px] checked:border-primary-500 focus-visible:ring-2 focus-visible:ring-primary-300 focus-visible:ring-offset-2"
            type="radio"
            name="guard-history-risk-filter"
            :value="filter.value"
            :checked="activeRiskFilter === filter.value"
            @change="setRiskFilter(filter.value)"
          />
          {{ filter.label }}
        </label>
      </fieldset>
    </BottomSheet>

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
