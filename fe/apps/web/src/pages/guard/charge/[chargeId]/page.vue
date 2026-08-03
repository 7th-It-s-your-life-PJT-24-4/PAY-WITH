<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import backIconUrl from '@/assets/icons/charge-detail-back.svg'
import { getMockGuardChargeHistory } from '@/mocks/guard-charge-history.mock'

const route = useRoute()
const router = useRouter()

const chargeHistory = computed(() =>
  getMockGuardChargeHistory(String(route.params.chargeId)),
)
const formatMoney = (value: number) =>
  new Intl.NumberFormat('ko-KR').format(value)

const detailRows = computed(() => {
  const history = chargeHistory.value
  if (!history) return []

  const createdAt = new Date(history.createdAt)
  const dateTimeText = `${createdAt.getFullYear()}년 ${createdAt.getMonth() + 1}월 ${createdAt.getDate()}일 ${String(createdAt.getHours()).padStart(2, '0')}:${String(createdAt.getMinutes()).padStart(2, '0')}`

  return [
    { label: '충전금액', value: `-${formatMoney(history.amount)}원` },
    { label: '받는 분', value: history.wardName },
    { label: '출금처', value: `${history.bankName} ${history.accountSuffix}` },
    { label: '이체일시', value: dateTimeText },
  ]
})

function goBack() {
  router.replace({ name: 'guard-charge' })
}
</script>

<template>
  <main class="min-h-screen bg-white">
    <header class="grid h-11 grid-cols-[44px_1fr_44px] items-center">
      <button
        class="flex size-11 items-center justify-center"
        type="button"
        aria-label="충전 내역으로 돌아가기"
        @click="goBack"
      >
        <span class="flex size-6 items-center justify-center">
          <img
            class="h-[15.84px] w-[7.097px]"
            :src="backIconUrl"
            alt=""
            aria-hidden="true"
          />
        </span>
      </button>
      <h1
        class="text-center text-[20px] font-semibold leading-[1.2] tracking-[-0.4px] text-black"
      >
        충전 상세
      </h1>
      <span aria-hidden="true" />
    </header>

    <section
      v-if="detailRows.length"
      class="mt-[38px] grid gap-10 px-mobile-gutter"
      aria-label="충전 상세 정보"
    >
      <div
        v-for="row in detailRows"
        :key="row.label"
        class="flex min-h-6 items-center justify-between gap-md text-[20px] leading-[1.2] tracking-[-0.4px]"
      >
        <dt class="font-medium text-gray-700">{{ row.label }}</dt>
        <dd class="text-right font-semibold text-black">{{ row.value }}</dd>
      </div>
    </section>
  </main>
</template>
