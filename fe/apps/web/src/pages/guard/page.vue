<script setup lang="ts">
import { ConfirmModal } from '@pay-with/ui'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  mockGuardSeniors,
  mockGuardTransactions,
} from '@/mocks/guard-home.mock'
import GuardAssetCard from '@/pages/guard/-components/GuardAssetCard.vue'
import GuardRiskTransactionAlert from '@/pages/guard/-components/GuardRiskTransactionAlert.vue'
import GuardSeniorAvatarList from '@/pages/guard/-components/GuardSeniorAvatarList.vue'
import GuardTransactionList from '@/pages/guard/-components/GuardTransactionList.vue'
import { useGuardStore } from '@/stores/guard.store'
import { usePairingStore } from '@/stores/pairing.store'

const router = useRouter()
const guardStore = useGuardStore()
const pairingStore = usePairingStore()
const isPairingConfirmOpen = ref(false)
const isRiskTransactionAlertVisible = ref(true)
const recentGuardTransactions = mockGuardTransactions.slice(0, 3)
const dangerTransactions = mockGuardTransactions.filter(
  ({ status }) => status === 'danger',
)
const dangerTransactionCount = dangerTransactions.length
const firstDangerTransactionId = dangerTransactions[0]?.id ?? null

async function startPairing() {
  const issued = await pairingStore.issueCode()
  if (!issued) return

  isPairingConfirmOpen.value = false
  router.push({ name: 'guard-pairing-code' })
}
</script>

<template>
  <main class="min-h-screen pb-[calc(66px+env(safe-area-inset-bottom))]">
    <section
      v-if="!pairingStore.isPaired"
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
        :seniors="mockGuardSeniors"
        :active-senior-id="guardStore.activeSeniorId"
        @add="isPairingConfirmOpen = true"
        @select="guardStore.selectSenior"
      />

      <GuardAssetCard
        class="mt-md"
        :senior-name="guardStore.activeSenior?.name ?? ''"
        balance="1,000,000"
        @add-safe-account="router.push({ name: 'guard-safe-account' })"
      />

      <GuardRiskTransactionAlert
        v-if="
          isRiskTransactionAlertVisible &&
          dangerTransactionCount > 0 &&
          firstDangerTransactionId
        "
        :count="dangerTransactionCount"
        :senior-name="guardStore.activeSenior?.name ?? ''"
        @close="isRiskTransactionAlertVisible = false"
        @confirm="
          router.push({
            name: 'guard-transaction-detail',
            params: { transactionId: firstDangerTransactionId },
          })
        "
      />

      <GuardTransactionList
        class="mt-md"
        :transactions="recentGuardTransactions"
        @more="router.push({ name: 'guard-history' })"
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
