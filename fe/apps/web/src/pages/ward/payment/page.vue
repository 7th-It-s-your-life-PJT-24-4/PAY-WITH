<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import PaymentQrPanel from '@/pages/ward/payment/-components/PaymentQrPanel.vue'

const router = useRouter()
const balance = '130,000'
const initialSeconds = 59
const remainingSeconds = ref(initialSeconds)
let timer: ReturnType<typeof globalThis.setInterval> | undefined

const expired = computed(() => remainingSeconds.value === 0)
const timerLabel = computed(
  () => `00:${String(remainingSeconds.value).padStart(2, '0')}`,
)

function startTimer() {
  if (timer) globalThis.clearInterval(timer)
  timer = globalThis.setInterval(() => {
    if (remainingSeconds.value > 0) remainingSeconds.value -= 1
    if (remainingSeconds.value === 0 && timer) globalThis.clearInterval(timer)
  }, 1000)
}

function reissueQrCode() {
  remainingSeconds.value = initialSeconds
  startTimer()
}

function proceedToPassword() {
  router.push({ name: 'ward-payment-password' })
}

onMounted(startTimer)
onBeforeUnmount(() => {
  if (timer) globalThis.clearInterval(timer)
})
</script>

<template>
  <div class="flex flex-col gap-xl">
    <section class="text-center">
      <h2 class="type-h1 text-body">매장에서<br />QR코드를 보여주세요</h2>
      <p class="type-body-medium mt-xs text-body-secondary">
        직원이 QR코드를 스캔하면 결제가 완료됩니다.
      </p>
    </section>

    <div class="relative">
      <PaymentQrPanel
        :expired="expired"
        @scan="proceedToPassword"
        @reissue="reissueQrCode"
      />
      <p
        v-if="!expired"
        class="type-h4 absolute -bottom-sm left-1/2 -translate-x-1/2 whitespace-nowrap rounded-full bg-gray-900 px-md py-xs text-body"
        aria-live="polite"
      >
        유효시간 {{ timerLabel }}
      </p>
    </div>

    <section
      class="rounded-large border border-primary-500/20 bg-primary-500/10 p-lg text-center text-primary-300"
      aria-labelledby="payment-balance-title"
    >
      <h2 id="payment-balance-title" class="type-body-medium">
        결제 가능 잔액
      </h2>
      <p class="type-amount mt-xs">{{ balance }}원</p>
    </section>

    <Button
      class="mt-auto w-full"
      label="내역 확인"
      variant="outline-primary"
      size="default"
    />
  </div>
</template>
