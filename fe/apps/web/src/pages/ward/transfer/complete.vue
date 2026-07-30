<script setup lang="ts">
import { CircleCheckBig } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed } from 'vue'
import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router'

import { useTransferStatus } from '@/composables/useTransferStatus'
import { formatTransferDateTime } from '@/pages/ward/transfer/-utils/transfer-status-route'
import { useTransferStore } from '@/stores/transfer.store'

const route = useRoute()
const router = useRouter()
const transferStore = useTransferStore()
const transactionId = computed(() => Number(route.params.transactionId))
const { transferDetail } = useTransferStatus(transactionId)
const formatMoney = (value: number) =>
  new Intl.NumberFormat('ko-KR').format(value)

async function goHome() {
  await router.replace({ name: 'ward-home' })
  transferStore.reset()
}

onBeforeRouteLeave((to) => {
  if (to.name === 'ward-home') return true

  transferStore.reset()
  return { name: 'ward-home', replace: true }
})
</script>

<template>
  <div class="flex flex-col gap-md">
    <section class="rounded-large bg-surface-card p-xl shadow-card">
      <div class="text-center">
        <p
          class="type-h4 inline-flex items-center gap-xs rounded-full bg-primary-900 px-md py-xs text-primary-500"
        >
          <CircleCheckBig
            class="size-lg"
            :stroke-width="2.5"
            aria-hidden="true"
          />
          송금 완료
        </p>
        <h2 class="type-amount mt-md">
          <span class="font-number">{{
            formatMoney(transferDetail?.amount ?? 0)
          }}</span
          ><span class="type-h2">원</span>
        </h2>
      </div>
      <div class="mt-xl flex items-center gap-md border-t border-border pt-md">
        <span
          class="type-h2 flex size-12 items-center justify-center rounded-full bg-primary-900 text-primary-300"
          >{{ transferDetail?.holderName.slice(0, 1) }}</span
        >
        <div>
          <h3 class="type-h4">{{ transferDetail?.holderName }}</h3>
          <p class="type-body-medium text-body-muted">
            {{ transferDetail?.bankName }} {{ transferDetail?.accountNo }}
          </p>
        </div>
      </div>
    </section>

    <section class="rounded-large bg-surface-card p-xl shadow-card">
      <dl class="grid gap-xl">
        <div class="flex justify-between">
          <dt>송금 일시</dt>
          <dd>
            <span class="font-number">{{
              transferDetail?.completedAt
                ? formatTransferDateTime(transferDetail.completedAt)
                : '-'
            }}</span>
          </dd>
        </div>
        <div class="flex justify-between">
          <dt>결제 수단</dt>
          <dd>PayWith 머니</dd>
        </div>
        <div>
          <dt>메모</dt>
          <dd class="mt-sm rounded-medium bg-gray-900 p-md">
            {{ transferDetail?.memo || '메모 없음' }}
          </dd>
        </div>
      </dl>
    </section>

    <section
      class="rounded-large border border-primary-500/30 bg-primary-500/10 p-xl"
    >
      <h3 class="type-h4 text-primary-300">안전하게 거래 보호 중</h3>
      <p class="type-body-medium mt-xs text-body-secondary">
        보호자님이 확인한 안전한 거래입니다. PayWith 보안 엔진이 24시간 자산을
        지키고 있습니다.
      </p>
    </section>

    <Button
      class="mt-lg w-full"
      label="홈으로"
      size="large"
      pill
      @click="goHome"
    />
  </div>
</template>
