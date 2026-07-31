<script setup lang="ts">
import { ConfirmModal } from '@pay-with/ui'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  mockGuardSeniors,
  mockGuardTransactions,
  type GuardTransaction,
} from '@/mocks/guard-home.mock'
import GuardSeniorAvatarList from '@/pages/guard/-components/GuardSeniorAvatarList.vue'
import GuardHistoryTransactionList from '@/pages/guard/history/-components/GuardHistoryTransactionList.vue'
import { useGuardStore } from '@/stores/guard.store'
import { usePairingStore } from '@/stores/pairing.store'

type HistoryFilter = 'all' | GuardTransaction['status']

const filters: Array<{ label: string; value: HistoryFilter }> = [
  { label: '전체', value: 'all' },
  { label: '위험', value: 'danger' },
  { label: '주의', value: 'warning' },
  { label: '안전', value: 'safe' },
]

const router = useRouter()
const guardStore = useGuardStore()
const pairingStore = usePairingStore()
const activeFilter = ref<HistoryFilter>('all')
const isPairingConfirmOpen = ref(false)

const filteredTransactions = computed(() => {
  if (activeFilter.value === 'all') return mockGuardTransactions
  return mockGuardTransactions.filter(
    ({ status }) => status === activeFilter.value,
  )
})

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
        :seniors="mockGuardSeniors"
        :active-senior-id="guardStore.activeSeniorId"
        @add="isPairingConfirmOpen = true"
        @select="guardStore.selectSenior"
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

      <GuardHistoryTransactionList
        class="mt-md"
        :transactions="filteredTransactions"
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
