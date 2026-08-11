<script setup lang="ts">
import { PinKeypad } from '@pay-with/ui'
import { useMutation } from '@tanstack/vue-query'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { getApiErrorMessage } from '@/api/error'
import { createWardPayment } from '@/api/payments'
import { usePaymentStore } from '@/stores/payment.store'

const router = useRouter()
const paymentStore = usePaymentStore()
const createPayment = useMutation({ mutationFn: createWardPayment })
const keypad = ref<{ reset: () => void } | null>(null)
const errorMessage = ref('')

async function createQrCode(pin: string) {
  if (createPayment.isPending.value) return
  errorMessage.value = ''

  try {
    const session = await createPayment.mutateAsync({ pin })
    paymentStore.saveQrSession(session)
    await router.replace({
      name: 'ward-payment-qr',
      params: { paymentId: session.paymentId },
    })
  } catch (error) {
    errorMessage.value = await getApiErrorMessage(
      error,
      'QR 코드를 만들지 못했습니다. 다시 시도해주세요.',
    )
    keypad.value?.reset()
  }
}
</script>

<template>
  <div
    class="flex min-h-[calc(100vh-var(--spacing-header)-var(--spacing-xl))] flex-col"
  >
    <section>
      <h2 class="type-h1 text-body">
        안전한 결제를 위해<br />비밀번호를 입력해주세요
      </h2>
    </section>

    <p
      v-if="errorMessage"
      class="type-body-medium mt-lg text-center text-error"
      role="alert"
    >
      {{ errorMessage }}
    </p>

    <section
      class="-mx-mobile-gutter mt-auto rounded-t-[32px] bg-surface-card px-xl pb-xl pt-[72px] shadow-[0_-8px_12px_rgb(0_0_0/4%)]"
      aria-label="결제 비밀번호 키패드"
    >
      <PinKeypad
        ref="keypad"
        :disabled="createPayment.isPending.value"
        @complete="createQrCode"
      />
      <p
        v-if="createPayment.isPending.value"
        class="type-body-medium mt-lg text-center text-primary-300"
        role="status"
      >
        결제 QR 코드를 만들고 있습니다.
      </p>
    </section>
  </div>
</template>
