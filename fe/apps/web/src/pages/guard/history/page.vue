<script setup lang="ts">
import { ConfirmModal } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { guardHomeOptions } from '@/lib/query/guard/home'
import { guardTransactionHistoryOptions } from '@/lib/query/transaction-history'
import GuardSeniorAvatarList from '@/pages/guard/-components/GuardSeniorAvatarList.vue'
import GuardHistoryTransactionList from '@/pages/guard/history/-components/GuardHistoryTransactionList.vue'
import type { TransactionRiskLevel } from '@/schemas/transaction-history.schema'
import { usePairingStore } from '@/stores/pairing.store'

type HistoryFilter = 'ALL' | TransactionRiskLevel

const filters: Array<{ label: string; value: HistoryFilter }> = [
  { label: '전체', value: 'ALL' },
  { label: '위험', value: 'DANGER' },
  { label: '주의', value: 'CAUTION' },
  { label: '안전', value: 'SAFE' },
]

const router = useRouter()
const pairingStore = usePairingStore()
const activeFilter = ref<HistoryFilter>('ALL')
const isPairingConfirmOpen = ref(false)
const selectedWardId = ref<number | null>(null)
const page = ref(0)
const size = 20

const guardHomeQuery = useQuery(guardHomeOptions(selectedWardId))
const selectedWard = computed(
  () => guardHomeQuery.data.value?.selectedWard ?? null,
)
const effectiveWardId = computed(() => selectedWard.value?.wardId ?? null)
const seniors = computed(() =>
  (guardHomeQuery.data.value?.wards ?? []).map(({ wardId, name }) => ({
    id: String(wardId),
    name,
  })),
)
const transactionParams = computed(() => ({
  riskLevel: activeFilter.value === 'ALL' ? undefined : activeFilter.value,
  page: page.value,
  size,
}))
const transactionQuery = useQuery(
  guardTransactionHistoryOptions(effectiveWardId, transactionParams),
)
const isLoading = computed(
  () =>
    guardHomeQuery.isPending.value ||
    (effectiveWardId.value !== null && transactionQuery.isPending.value),
)
const hasError = computed(
  () => guardHomeQuery.isError.value || transactionQuery.isError.value,
)

function selectSenior(wardId: string) {
  const parsedWardId = Number(wardId)
  if (!Number.isSafeInteger(parsedWardId) || parsedWardId <= 0) return
  selectedWardId.value = parsedWardId
  page.value = 0
}

function selectFilter(filter: HistoryFilter) {
  activeFilter.value = filter
  page.value = 0
}

function refetchHistory() {
  void guardHomeQuery.refetch()
  void transactionQuery.refetch()
}

async function startPairing() {
  const issued = await pairingStore.issueCode()
  if (!issued) return

  isPairingConfirmOpen.value = false
  router.push({ name: 'guard-pairing-code' })
}
</script>

<template>
  <main class="min-h-screen pb-[calc(66px+env(safe-area-inset-bottom))]">
    <div class="px-mobile-gutter pt-md">
      <GuardSeniorAvatarList
        :seniors="seniors"
        :active-senior-id="effectiveWardId ? String(effectiveWardId) : ''"
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
          @click="selectFilter(filter.value)"
        >
          {{ filter.label }}
        </button>
      </div>

      <p
        v-if="isLoading"
        class="mt-xl text-center text-[14px] font-medium text-gray-500"
        aria-live="polite"
      >
        거래 내역을 불러오는 중이에요.
      </p>
      <section v-else-if="hasError" class="mt-xl text-center" role="alert">
        <p class="text-[14px] font-medium text-gray-500">
          거래 내역을 불러오지 못했어요.
        </p>
        <button
          type="button"
          class="mt-md rounded-medium bg-primary-500 px-md py-sm text-[14px] font-semibold text-white"
          @click="refetchHistory"
        >
          다시 시도
        </button>
      </section>
      <GuardHistoryTransactionList
        v-else-if="transactionQuery.data.value?.transactions.length"
        class="mt-md"
        :transactions="transactionQuery.data.value.transactions"
      />
      <p v-else class="mt-xl text-center text-[14px] font-medium text-gray-500">
        거래 내역이 없습니다.
      </p>

      <nav
        v-if="transactionQuery.data.value?.transactions.length"
        class="mt-lg flex items-center justify-center gap-md"
        aria-label="거래 내역 페이지"
      >
        <button
          type="button"
          class="rounded-medium border border-gray-300 px-md py-sm text-[14px] font-semibold disabled:opacity-40"
          :disabled="page === 0"
          @click="page -= 1"
        >
          이전
        </button>
        <span class="text-[12px] font-medium text-gray-500"
          >{{ page + 1 }}페이지</span
        >
        <button
          type="button"
          class="rounded-medium border border-gray-300 px-md py-sm text-[14px] font-semibold disabled:opacity-40"
          :disabled="!transactionQuery.data.value?.hasNext"
          @click="page += 1"
        >
          다음
        </button>
      </nav>
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
