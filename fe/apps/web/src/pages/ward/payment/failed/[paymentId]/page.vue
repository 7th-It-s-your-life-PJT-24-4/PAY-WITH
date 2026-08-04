<script setup lang="ts">
import { Check, House, Store } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getApiErrorMessage } from '@/api/error'
import { useWardPaymentStatusQuery } from '@/composables/useWardPaymentStatusQuery'
import { usePaymentStore } from '@/stores/payment.store'

const route = useRoute()
const router = useRouter()
const paymentStore = usePaymentStore()
const errorMessage = ref('')

const paymentId = computed(() => {
  const value = Number(route.params.paymentId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const paymentStatus = useWardPaymentStatusQuery(paymentId)
const payment = computed(() =>
  paymentStatus.data.value?.status === 'FAILED'
    ? paymentStatus.data.value
    : null,
)

async function goHome() {
  paymentStore.clear()
  await router.replace({ name: 'ward-home' })
}

watch(
  () => paymentStatus.error.value,
  async (error) => {
    errorMessage.value = error
      ? await getApiErrorMessage(error, '결제 실패 정보를 불러오지 못했습니다.')
      : ''
  },
  { immediate: true },
)
</script>

<template>
  <div class="flex flex-1 flex-col gap-section">
    <template v-if="payment">
      <section class="flex flex-col items-center gap-xl text-center">
        <div
          class="flex size-24 items-center justify-center rounded-full bg-error text-on-semantic shadow-modal"
          aria-hidden="true"
        >
          <Check class="size-12" :stroke-width="5" />
        </div>
        <h2 class="type-h1">결제가 실패했습니다</h2>
      </section>

      <section class="flex flex-col gap-md" aria-label="결제 실패 정보">
        <article
          class="rounded-large border border-border bg-surface-card p-xl shadow-card"
        >
          <div
            class="flex items-start justify-between border-b border-border pb-md"
          >
            <div>
              <p class="type-h4 text-body-muted">사용처</p>
              <h3 class="type-h2 mt-xxs">
                {{ payment.merchantName || '사용처 정보 없음' }}
              </h3>
            </div>
            <Store
              class="size-7 text-action"
              :stroke-width="2.5"
              aria-hidden="true"
            />
          </div>
          <div class="pt-md">
            <p class="type-h4 text-body-muted">결제 금액</p>
            <p class="type-h2 mt-xxs text-primary-300">
              <span class="font-number">{{
                payment.amount?.toLocaleString('ko-KR') ?? '-'
              }}</span
              ><span v-if="payment.amount !== null">원</span>
            </p>
          </div>
        </article>

        <article
          class="flex items-center gap-md rounded-large border border-primary-300/10 bg-surface-card p-xl shadow-card"
        >
          <span
            class="type-h4 shrink-0 rounded-full bg-primary-800 px-md py-sm text-primary-50"
          >
            실패 이유
          </span>
          <p class="type-h2">
            {{ payment.failureMessage || '결제에 실패했습니다.' }}
          </p>
        </article>
      </section>

      <Button
        class="mt-auto w-full"
        label="홈으로"
        size="large"
        pill
        @click="goHome"
      >
        <template #leading>
          <House :stroke-width="2.5" />
        </template>
      </Button>
    </template>

    <p
      v-else-if="errorMessage"
      class="type-body-medium text-center text-error"
      role="alert"
    >
      {{ errorMessage }}
    </p>
    <p
      v-else
      class="type-body-medium text-center text-body-muted"
      role="status"
    >
      결제 실패 정보를 확인하고 있습니다.
    </p>
  </div>
</template>
