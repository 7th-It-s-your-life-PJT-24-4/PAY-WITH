<script setup lang="ts">
import { useQuery } from '@tanstack/vue-query'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import backIconUrl from '@/assets/icons/charge-detail-back.svg'
import { guardChargeDetailOptions } from '@/lib/query/guard/charge'
import {
  parsePositiveRouteId,
  withGuardWardId,
} from '@/pages/guard/-utils/guard-route'

const route = useRoute()
const router = useRouter()

const chargeId = parsePositiveRouteId(route.params.id) ?? 0
const chargeDetailQuery = useQuery(guardChargeDetailOptions(chargeId))
const formatMoney = (value: number) =>
  new Intl.NumberFormat('ko-KR').format(value)

const detailRows = computed(() => {
  const detail = chargeDetailQuery.data.value
  if (!detail) return []

  const createdAt = new Date(detail.createdAt)
  const dateTimeText = `${createdAt.getFullYear()}년 ${createdAt.getMonth() + 1}월 ${createdAt.getDate()}일 ${String(createdAt.getHours()).padStart(2, '0')}:${String(createdAt.getMinutes()).padStart(2, '0')}`

  return [
    { label: '충전금액', value: `-${formatMoney(detail.amount)}원` },
    { label: '받는 분', value: detail.wardName },
    {
      label: '출금처',
      value: `${detail.account.bankName} ${detail.account.accountNo.slice(-4)}`,
    },
    { label: '이체일시', value: dateTimeText },
  ]
})

function goBack() {
  router.replace({
    name: 'guard-charge',
    query: withGuardWardId({}, parsePositiveRouteId(route.query.wardId)),
  })
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
    <p
      v-else-if="chargeDetailQuery.isPending.value"
      class="type-body-medium mt-[38px] px-mobile-gutter text-body-secondary"
    >
      충전 내역을 불러오는 중이에요.
    </p>
    <p
      v-else
      class="type-body-medium mt-[38px] px-mobile-gutter text-error"
      role="alert"
    >
      충전 내역을 불러오지 못했어요.
    </p>
  </main>
</template>
