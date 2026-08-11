<script setup lang="ts">
import { CircleCheckBig } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { useChargeStore } from '@/stores/charge.store'
import { formatCompactAccount } from '@/pages/ward/charge/-utils/account-format'

const router = useRouter()
const chargeStore = useChargeStore()
const result = computed(() => chargeStore.result)
const formatMoney = (value: number) =>
  new Intl.NumberFormat('ko-KR').format(value)

function goHome() {
  chargeStore.resetDraft()
  router.replace({ name: 'ward-home' })
}
</script>

<template>
  <div v-if="result" class="flex flex-col gap-section">
    <section class="flex flex-col items-center text-center">
      <span
        class="flex size-24 items-center justify-center rounded-full bg-action text-on-action shadow-modal"
        aria-hidden="true"
      >
        <CircleCheckBig class="size-12" :stroke-width="2.5" />
      </span>
      <h2 class="type-h1 mt-xl">충전이 완료되었습니다</h2>
    </section>

    <article
      class="divide-y divide-border rounded-large border border-border bg-surface-card px-xl shadow-card"
      aria-label="충전 결과"
    >
      <div class="flex items-center justify-between gap-md py-lg">
        <span class="type-h4 text-body-muted">충전 금액</span>
        <strong class="type-h2 text-primary-500">
          <span class="font-number">{{ formatMoney(result.chargeAmount) }}</span
          >원
        </strong>
      </div>
      <div class="flex items-center justify-between gap-md py-lg">
        <span class="type-h4 text-body-muted">충전 수단</span>
        <strong class="type-h2">
          {{ formatCompactAccount(result.bankName, result.accountNo) }}
        </strong>
      </div>
      <div class="flex items-center justify-between gap-md py-lg">
        <span class="type-h4 text-body-muted">최종 잔액</span>
        <strong class="type-h2">
          <span class="font-number">{{ formatMoney(result.balanceAfter) }}</span
          >원
        </strong>
      </div>
    </article>

    <Button class="w-full" label="홈으로" size="large" @click="goHome" />
  </div>
</template>
