<script setup lang="ts">
import { Check } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { usePaymentStore } from '@/stores/payment.store'

const router = useRouter()
const paymentStore = usePaymentStore()
paymentStore.restore()

const payment = computed(() => paymentStore.completedPayment)

function formatPaidAt(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    month: 'long',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  }).format(new Date(value))
}

async function goHome() {
  paymentStore.clear()
  await router.replace({ name: 'ward-home' })
}
</script>

<template>
  <div v-if="payment" class="flex flex-col gap-section">
    <section class="flex flex-col items-center gap-xl text-center">
      <div
        class="flex size-24 items-center justify-center rounded-full bg-action text-on-action shadow-modal"
        aria-hidden="true"
      >
        <Check class="size-12" :stroke-width="5" />
      </div>
      <h2 class="type-h1">결제가 완료되었습니다</h2>
    </section>

    <section class="flex flex-col gap-md" aria-label="결제 정보">
      <article
        class="rounded-large border border-border bg-surface-card p-xl shadow-card"
      >
        <div class="border-b border-border pb-md">
          <p class="type-h4 text-body-muted">사용처</p>
          <h3 class="type-h2 mt-xxs">{{ payment.merchantName }}</h3>
        </div>
        <div class="flex justify-between gap-md pt-md">
          <div>
            <p class="type-h4 text-body-muted">결제 금액</p>
            <p class="type-h2 mt-xxs text-primary-300">
              <span class="font-number">{{
                payment.amount.toLocaleString('ko-KR')
              }}</span
              >원
            </p>
          </div>
          <div class="text-right">
            <p class="type-h4 text-body-muted">결제 시간</p>
            <p class="type-h4 mt-xxs font-number text-body">
              {{ formatPaidAt(payment.paidAt) }}
            </p>
          </div>
        </div>
      </article>

      <article
        class="flex items-center gap-md rounded-large border border-primary-300/10 bg-surface-card p-xl shadow-card"
      >
        <span
          class="flex size-12 items-center justify-center rounded-full bg-primary-800 text-primary-300"
          aria-hidden="true"
          >₩</span
        >
        <div>
          <p class="type-h4 text-body-muted">남은 잔액</p>
          <p class="type-h3">
            <span class="font-number">{{
              payment.remainingBalance.toLocaleString('ko-KR')
            }}</span
            >원
          </p>
        </div>
      </article>
    </section>

    <Button class="w-full" label="홈으로" size="large" pill @click="goHome" />
  </div>
</template>
