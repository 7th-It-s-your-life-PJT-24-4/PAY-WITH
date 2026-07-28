<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { useRouter } from 'vue-router'

import WardBalanceCard from '@/pages/ward/-components/WardBalanceCard.vue'

const router = useRouter()

const wardName = '김시니어'
const balance = '100,000'
const isWalletLocked = false

const actions = [
  { label: '송금하기', value: 'transfer' },
  { label: '충전하기', value: 'charge' },
  { label: '결제하기', value: 'payment' },
  { label: '내역 조회', value: 'history' },
]

function handleAction(value: string) {
  if (value === 'transfer') router.push({ name: 'ward-transfer' })
  if (value === 'charge') router.push({ name: 'ward-charge' })
  if (value === 'payment') router.push({ name: 'ward-payment' })
  if (value === 'history') router.push({ name: 'ward-transaction-history' })
}
</script>

<template>
  <div class="flex flex-col gap-xl">
    <section aria-labelledby="ward-welcome-title">
      <p class="type-body-medium text-body-secondary">환영합니다</p>
      <h1 id="ward-welcome-title" class="type-h1 mt-xs text-body">
        <span class="text-primary-500">{{ wardName }}</span
        >님 안녕하세요
      </h1>
    </section>

    <WardBalanceCard :balance="balance" :locked="isWalletLocked" />

    <section class="grid gap-md" aria-label="홈 주요 기능">
      <Button
        v-for="action in actions"
        :key="action.value"
        class="w-full"
        :label="action.label"
        variant="outline-primary"
        size="large"
        @click="handleAction(action.value)"
      />
    </section>
  </div>
</template>
