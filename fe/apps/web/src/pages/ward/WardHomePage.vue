<script setup lang="ts">
import { AppHeader, Button } from '@pay-with/ui'
import { useRouter } from 'vue-router'

import WardBalanceCard from '@/pages/ward/components/WardBalanceCard.vue'
import WardBottomNavigation from '@/pages/ward/components/WardBottomNavigation.vue'

const router = useRouter()

const wardName = '김시니어'
const balance = '100,000'
const actions = ['송금하기', '충전하기', '결제하기', '내역 조회']

function goBack() {
  router.back()
}
</script>

<template>
  <div class="min-h-screen bg-gray-900 px-0 sm:px-md">
    <div
      class="mx-auto flex min-h-screen w-full max-w-[390px] flex-col bg-surface text-body shadow-card"
    >
      <AppHeader title="PayWith" show-back show-profile @back="goBack" />

      <main class="flex flex-1 flex-col gap-xl px-mobile-gutter py-xl">
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
      </main>

      <WardBottomNavigation active="payment" />
    </div>
  </div>
</template>
