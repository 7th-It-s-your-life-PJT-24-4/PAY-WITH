<script setup lang="ts">
import { Clock3, CreditCard, Phone, ShieldCheck, X } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  cancelMockHeldPayment,
  getMockPendingTransaction,
} from '@/mocks/pending-transaction.mock'
import PaymentCancelModal from '@/pages/ward/payment/-components/PaymentCancelModal.vue'
import TransferGuardianCallModal from '@/pages/ward/transfer/-components/TransferGuardianCallModal.vue'

const route = useRoute()
const router = useRouter()
const isGuardianCallModalOpen = ref(false)
const isCancelModalOpen = ref(false)
const isCancelling = ref(false)
const errorMessage = ref('')
const transaction = computed(() => {
  const pendingTransaction = getMockPendingTransaction(
    Number(route.params.transactionId),
  )
  return pendingTransaction?.type === 'PAYMENT' ? pendingTransaction : null
})

function formatAmount(amount: number) {
  return `${amount.toLocaleString('ko-KR')}원`
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(new Date(value))
}

async function confirmCancel() {
  if (!transaction.value || isCancelling.value) return
  isCancelling.value = true
  errorMessage.value = ''
  try {
    await cancelMockHeldPayment(transaction.value.transactionId)
    isCancelModalOpen.value = false
    await router.replace({ name: 'ward-home' })
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '결제를 취소하지 못했습니다.'
  } finally {
    isCancelling.value = false
  }
}
</script>

<template>
  <div v-if="transaction" class="flex min-h-full flex-col gap-xl">
    <section class="text-center" aria-labelledby="payment-held-title">
      <span
        class="mx-auto flex size-20 items-center justify-center rounded-full bg-warning/15 text-warning"
        aria-hidden="true"
      >
        <Clock3 class="size-11" :stroke-width="2.25" />
      </span>
      <h2 id="payment-held-title" class="type-h1 mt-lg text-body">
        결제 승인을 기다리고 있어요
      </h2>
      <p class="type-h4 mt-xs text-body-secondary">
        보호자가 확인하면 결과를 알려드릴게요.
      </p>
    </section>

    <section
      class="rounded-large border border-border bg-surface-card p-xl shadow-card"
    >
      <div class="flex items-center gap-md border-b border-border pb-lg">
        <span
          class="flex size-12 items-center justify-center rounded-full bg-primary-900 text-primary-300"
          aria-hidden="true"
        >
          <CreditCard class="size-6" />
        </span>
        <div>
          <p class="type-body-medium text-body-muted">결제 가맹점</p>
          <h3 class="type-h3 text-body">{{ transaction.merchantName }}</h3>
        </div>
      </div>

      <dl class="mt-lg grid gap-lg">
        <div class="flex items-center justify-between gap-md">
          <dt class="type-body-medium text-body-muted">결제 금액</dt>
          <dd class="type-h2 font-number text-primary-300">
            {{ formatAmount(transaction.amount) }}
          </dd>
        </div>
        <div class="flex items-center justify-between gap-md">
          <dt class="type-body-medium text-body-muted">결제 수단</dt>
          <dd class="type-body-medium text-body">
            {{ transaction.paymentMethod }}
          </dd>
        </div>
        <div class="flex items-center justify-between gap-md">
          <dt class="type-body-medium text-body-muted">요청 시간</dt>
          <dd class="type-body-medium font-number text-body">
            {{ formatDateTime(transaction.requestedAt) }}
          </dd>
        </div>
      </dl>
    </section>

    <section
      class="rounded-large border border-primary-500/30 bg-primary-500/10 p-lg text-primary-200"
    >
      <div class="flex gap-md">
        <ShieldCheck
          class="size-xl shrink-0"
          :stroke-width="2.25"
          aria-hidden="true"
        />
        <div>
          <h3 class="type-h4">안전하게 확인하고 있어요</h3>
          <p class="type-body-medium mt-xxs">
            승인 전에는 결제가 완료되지 않습니다.
          </p>
        </div>
      </div>

      <Button
        class="mt-lg w-full !gap-sm !px-md"
        label="보호자에게 연락하기"
        variant="outline-primary"
        size="default"
        pill
        @click="isGuardianCallModalOpen = true"
      >
        <template #leading>
          <Phone :stroke-width="2.5" />
        </template>
      </Button>
    </section>

    <div class="mt-auto flex flex-col gap-md pt-lg">
      <Button
        class="w-full"
        label="홈에서 기다리기"
        size="large"
        @click="router.push({ name: 'ward-home' })"
      />
      <Button
        class="w-full !gap-sm !px-md"
        :label="isCancelling ? '결제를 취소하고 있습니다' : '결제 취소하기'"
        variant="outline-danger"
        size="large"
        :disabled="isCancelling"
        @click="isCancelModalOpen = true"
      >
        <template #leading>
          <X :stroke-width="2.5" />
        </template>
      </Button>
      <p
        v-if="errorMessage"
        class="type-body-medium text-center text-error"
        role="alert"
      >
        {{ errorMessage }}
      </p>
    </div>

    <TransferGuardianCallModal v-model:open="isGuardianCallModalOpen" />
    <PaymentCancelModal
      v-model:open="isCancelModalOpen"
      :cancelling="isCancelling"
      @confirm="confirmCancel"
    />
  </div>
</template>
