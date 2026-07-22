<script setup lang="ts">
import { Button } from '@pay-with/ui'

import WardBalanceCard from '@/pages/ward/-components/WardBalanceCard.vue'

const wardName = '김시니어'
const balance = '100,000'
const actions = ['송금하기', '충전하기', '결제하기', '내역 조회']
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

    <WardBalanceCard :balance="balance" />

    <section class="grid gap-md" aria-label="홈 주요 기능">
      <Button
        v-for="action in actions"
        :key="action"
        class="w-full"
        :label="action"
        variant="outline-primary"
        size="large"
      />
    </section>
  </div>
</template>
