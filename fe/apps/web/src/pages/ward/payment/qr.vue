<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'

import { getApiErrorMessage } from '@/api/error'
import { useCancelWardPaymentMutation } from '@/composables/useCancelWardPaymentMutation'
import { useWardPaymentStatusQuery } from '@/composables/useWardPaymentStatusQuery'
import PaymentQrCancelModal from '@/pages/ward/payment/-components/PaymentQrCancelModal.vue'
import PaymentQrPanel from '@/pages/ward/payment/-components/PaymentQrPanel.vue'
import { completedPaymentSchema } from '@/schemas/payment.schema'
import { usePaymentStore } from '@/stores/payment.store'

const route = useRoute()
const router = useRouter()
const paymentStore = usePaymentStore()
paymentStore.restore()

const paymentId = computed(() => {
  const value = Number(route.params.paymentId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const paymentStatus = useWardPaymentStatusQuery(paymentId)
const cancelPayment = useCancelWardPaymentMutation()
const now = ref(Date.now())
const isCancelModalOpen = ref(false)
const errorMessage = ref('')
const allowLeave = ref(false)
let pendingDestination = ''
let timer: ReturnType<typeof globalThis.setInterval> | undefined

const session = computed(() => paymentStore.qrSession)
const remainingSeconds = computed(() => {
  if (!session.value) return 0
  return Math.max(
    0,
    Math.ceil((Date.parse(session.value.expiresAt) - now.value) / 1_000),
  )
})
const expired = computed(
  () =>
    remainingSeconds.value === 0 ||
    paymentStatus.data.value?.status === 'EXPIRED',
)
const timerLabel = computed(() => {
  const minutes = Math.floor(remainingSeconds.value / 60)
  const seconds = remainingSeconds.value % 60
  return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`
})
const isProcessing = computed(
  () => paymentStatus.data.value?.status === 'PROCESSING',
)
const failed = computed(() => paymentStatus.data.value?.status === 'FAILED')

function startTimer() {
  timer = globalThis.setInterval(() => {
    now.value = Date.now()
  }, 1_000)
}

async function reissueQrCode() {
  paymentStore.clearQrSession()
  allowLeave.value = true
  await router.replace({ name: 'ward-payment' })
}

async function goHome() {
  paymentStore.clearQrSession()
  allowLeave.value = true
  await router.replace({ name: 'ward-home' })
}

async function confirmCancel() {
  if (!paymentId.value || cancelPayment.isPending.value) return
  errorMessage.value = ''
  try {
    await cancelPayment.mutateAsync(paymentId.value)
    paymentStore.clearQrSession()
    isCancelModalOpen.value = false
    allowLeave.value = true
    await router.push(pendingDestination || { name: 'ward-home' })
  } catch (error) {
    errorMessage.value = await getApiErrorMessage(
      error,
      '결제를 취소하지 못했습니다.',
    )
  }
}

watch(
  () => paymentStatus.data.value,
  async (payment) => {
    if (!payment) return

    if (payment.status === 'COMPLETED') {
      const completed = completedPaymentSchema.safeParse(payment)
      if (!completed.success) {
        errorMessage.value = '완료된 결제 정보를 확인하지 못했습니다.'
        return
      }
      paymentStore.saveCompletedPayment(completed.data)
      allowLeave.value = true
      await router.replace({
        name: 'ward-payment-complete',
        params: { transactionId: completed.data.transactionId },
      })
      return
    }

    if (payment.status === 'CANCELED') {
      paymentStore.clearQrSession()
      allowLeave.value = true
      await router.replace({ name: 'ward-home' })
      return
    }

    if (payment.status === 'FAILED') {
      errorMessage.value = payment.failureMessage || '결제에 실패했습니다.'
      allowLeave.value = true
    }
  },
  { immediate: true },
)

watch(
  () => paymentStatus.error.value,
  async (error) => {
    if (error)
      errorMessage.value = await getApiErrorMessage(
        error,
        '결제 상태를 확인하지 못했습니다.',
      )
  },
)

onBeforeRouteLeave((to) => {
  if (allowLeave.value || expired.value || failed.value) return true
  if (isProcessing.value) return false

  pendingDestination = to.fullPath
  isCancelModalOpen.value = true
  return false
})

startTimer()
onBeforeUnmount(() => {
  if (timer) globalThis.clearInterval(timer)
})
</script>

<template>
  <div v-if="session" class="flex flex-col gap-xl">
    <section class="text-center">
      <h2 class="type-h1 text-body">매장에서<br />QR코드를 보여주세요</h2>
      <p class="type-body-medium mt-xs text-body-secondary">
        직원이 QR코드를 스캔하면 결제가 진행됩니다.
      </p>
    </section>

    <div class="relative">
      <PaymentQrPanel
        :payment-token="session.paymentToken"
        :expired="expired"
        :processing="isProcessing"
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
      <p class="type-amount mt-xs font-number">
        {{ session.availableBalance.toLocaleString('ko-KR') }}원
      </p>
    </section>

    <p
      v-if="errorMessage"
      class="type-body-medium text-center text-error"
      role="alert"
    >
      {{ errorMessage }}
    </p>

    <div v-if="failed" class="mt-auto flex flex-col gap-md">
      <Button
        class="w-full"
        label="다시 결제하기"
        size="large"
        @click="reissueQrCode"
      />
      <Button
        class="w-full"
        label="홈으로"
        variant="outline-primary"
        @click="goHome"
      />
    </div>
    <Button
      v-else-if="!expired"
      class="mt-auto w-full"
      label="결제 취소하기"
      variant="outline-danger"
      size="default"
      :disabled="isProcessing"
      @click="isCancelModalOpen = true"
    />

    <PaymentQrCancelModal
      v-model:open="isCancelModalOpen"
      :cancelling="cancelPayment.isPending.value"
      @confirm="confirmCancel"
    />
  </div>
</template>
