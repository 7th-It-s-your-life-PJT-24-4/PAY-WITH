<script setup lang="ts">
import { ConfirmModal } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { guardHomeOptions } from '@/lib/query/guard/home'
import { guardTransactionHistoryOptions } from '@/lib/query/guard/transactions'
import GuardSeniorAvatarList from '@/pages/guard/-components/GuardSeniorAvatarList.vue'
import { useGuardWardQuery } from '@/pages/guard/-composables/useGuardWardQuery'
import GuardHistoryTransactionList, {
  type GuardHistoryTransactionView,
} from '@/pages/guard/history/-components/GuardHistoryTransactionList.vue'
import type {
  GuardTransactionHistoryItem,
  TransactionRiskLevel,
} from '@/schemas/guard-transaction.schema'
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
const { selectedWardId, selectWard } = useGuardWardQuery()
const activeFilter = ref<HistoryFilter>('ALL')
const isPairingConfirmOpen = ref(false)
const selectedRiskLevel = computed<TransactionRiskLevel | null>(() =>
  activeFilter.value === 'ALL' ? null : activeFilter.value,
)

const guardHomeQuery = useQuery(guardHomeOptions(selectedWardId))
const transactionHistoryQuery = useQuery(
  guardTransactionHistoryOptions(selectedWardId, selectedRiskLevel),
)

const selectedWard = computed(
  () => guardHomeQuery.data.value?.selectedWard ?? null,
)
const seniors = computed(() =>
  (guardHomeQuery.data.value?.wards ?? []).map(
    ({ wardId, name, avatarId }) => ({
      id: String(wardId),
      name,
      imageUrl: `/images/avatar/avatar${avatarId}.png`,
    }),
  ),
)
const activeSeniorId = computed(() =>
  selectedWard.value ? String(selectedWard.value.wardId) : '',
)
const transactions = computed<GuardHistoryTransactionView[]>(() =>
  (transactionHistoryQuery.data.value?.transactions ?? []).map((transaction) =>
    toTransactionView(transaction, selectedWard.value?.name ?? '시니어'),
  ),
)

watch(
  () => selectedWard.value?.wardId,
  (wardId) => {
    if (!wardId) return
    selectWard(wardId)
  },
  { immediate: true },
)

function toTransactionView(
  transaction: GuardTransactionHistoryItem,
  wardName: string,
): GuardHistoryTransactionView {
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
  const counterparty = transaction.counterpartyName ?? '거래처'
  const description =
    transaction.type === 'CHARGE'
      ? `${counterparty} 충전`
      : transaction.type === 'TRANSFER'
        ? `${wardName} 계좌 → ${counterparty}`
        : counterparty
  const occurredAt = new Date(transaction.createdAt)
  const date = Number.isNaN(occurredAt.getTime())
    ? transaction.createdAt
    : new Intl.DateTimeFormat('ko-KR', {
        month: 'long',
        day: 'numeric',
      }).format(occurredAt)
  const amountPrefix = transaction.type === 'CHARGE' ? '+' : '-'

  return {
    id: transaction.transactionId,
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
  selectWard(parsedWardId)
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
          @click="activeFilter = filter.value"
        >
          {{ filter.label }}
        </button>
      </div>

      <p
        v-if="transactionHistoryQuery.isPending.value"
        class="py-16 text-center text-[16px] font-medium text-gray-500"
      >
        거래 내역을 불러오는 중이에요.
      </p>
      <p
        v-else-if="transactionHistoryQuery.isError.value"
        class="py-16 text-center text-[16px] font-medium text-gray-500"
        role="alert"
      >
        거래 내역을 불러오지 못했어요.
      </p>
      <p
        v-else-if="transactions.length === 0"
        class="py-16 text-center text-[16px] font-medium text-gray-700"
      >
        거래 내역이 없어요.
      </p>
      <GuardHistoryTransactionList
        v-else-if="selectedWardId"
        class="mt-md"
        :ward-id="selectedWardId"
        :transactions="transactions"
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
