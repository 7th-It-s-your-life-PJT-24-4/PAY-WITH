<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { useRouter } from 'vue-router'

const router = useRouter()

const payment = {
  merchant: '스타벅스 강남점',
  amount: '5,500',
  timePrefix: '오늘 오후',
  time: '2:45',
  balance: '124,500',
}
</script>

<template>
  <div class="flex flex-col gap-section">
    <section class="flex flex-col items-center gap-xl text-center">
      <div
        class="flex size-24 items-center justify-center rounded-full bg-action text-on-action shadow-modal"
        aria-hidden="true"
      >
        <svg class="size-12" viewBox="0 0 48 48" fill="none">
          <path
            d="m13 25 7 7 15-16"
            stroke="currentColor"
            stroke-width="5"
            stroke-linecap="round"
            stroke-linejoin="round"
          />
        </svg>
      </div>
      <h2 class="type-h1">결제가 완료되었습니다</h2>
    </section>

    <section class="flex flex-col gap-md" aria-label="결제 정보">
      <article
        class="rounded-large border border-border bg-surface-card p-xl shadow-card"
      >
        <div class="border-b border-border pb-md">
          <p class="type-h4 text-body-muted">사용처</p>
          <h3 class="type-h2 mt-xxs">{{ payment.merchant }}</h3>
        </div>
        <div class="flex justify-between gap-md pt-md">
          <div>
            <p class="type-h4 text-body-muted">결제 금액</p>
            <p class="type-h2 mt-xxs text-primary-300">
              <span class="font-number">{{ payment.amount }}</span
              >원
            </p>
          </div>
          <div class="text-right">
            <p class="type-h4 text-body-muted">결제 시간</p>
            <p class="type-h4 mt-xxs text-body">
              {{ payment.timePrefix }}
              <span class="font-number">{{ payment.time }}</span>
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
            <span class="font-number">{{ payment.balance }}</span
            >원
          </p>
        </div>
      </article>
    </section>

    <Button
      class="w-full"
      label="홈으로"
      size="large"
      pill
      @click="router.push({ name: 'ward-home' })"
    />
  </div>
</template>
