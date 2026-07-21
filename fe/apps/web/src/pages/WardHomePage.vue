<script setup lang="ts">
import { UiButton } from '@pay-with/ui'
import { useRouter } from 'vue-router'

import AppHeader from '@/components/AppHeader.vue'
import WardBottomNavigation from '@/components/ward/WardBottomNavigation.vue'
import WardBalanceCard from '@/components/ward/home/WardBalanceCard.vue'

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
          <UiButton
            v-for="action in actions"
            :key="action"
            class="type-h2 !min-h-button-large w-full !rounded-large !border-action !bg-surface-card !text-primary-500 hover:!bg-primary-900 focus-visible:!outline-focus"
            :label="action"
            variant="secondary"
          />
        </section>
      </main>

      <WardBottomNavigation active="payment" />
    </div>
  </div>
</template>
