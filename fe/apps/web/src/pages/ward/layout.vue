<script setup lang="ts">
import { AppHeader } from '@pay-with/ui'
import { useRouter } from 'vue-router'

import WardBottomNavigation from '@/pages/ward/-components/WardBottomNavigation.vue'

const router = useRouter()

function goBack() {
  router.back()
}
</script>

<template>
  <div class="min-h-screen bg-gray-900 px-0 sm:px-md">
    <div
      class="mx-auto flex min-h-screen w-full max-w-[390px] flex-col bg-surface text-body shadow-card"
    >
      <div
        class="fixed inset-x-0 top-0 z-40 mx-auto w-full max-w-[390px] bg-surface-card pt-[env(safe-area-inset-top)]"
      >
        <AppHeader title="PayWith" show-back show-profile @back="goBack" />
      </div>

      <main
        class="flex flex-1 flex-col px-mobile-gutter pb-[calc(var(--spacing-bottom-nav)+var(--spacing-section)+var(--spacing-xl)+env(safe-area-inset-bottom))] pt-[calc(var(--spacing-header)+var(--spacing-xl)+env(safe-area-inset-top))]"
      >
        <RouterView />
      </main>

      <WardBottomNavigation active="payment" />
    </div>
  </div>
</template>
