<script setup lang="ts">
import { AppHeader } from '@pay-with/ui'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import WardBottomNavigation from '@/pages/ward/-components/WardBottomNavigation.vue'

type WardNavigationValue = 'transfer' | 'payment' | 'history'

const router = useRouter()
const route = useRoute()

const headerTitle = computed(() => String(route.meta.title ?? 'PayWith'))
const showBottomNavigation = computed(
  () => route.meta.showBottomNavigation !== false,
)
const activeNavigation = computed<WardNavigationValue>(() => {
  const value = route.meta.activeNavigation
  return value === 'transfer' || value === 'history' ? value : 'payment'
})

function goBack() {
  router.back()
}

function handleNavigate(value: string) {
  if (value === 'payment') router.push({ name: 'ward-payment' })
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
        <AppHeader :title="headerTitle" show-back show-profile @back="goBack" />
      </div>

      <main
        class="flex flex-1 flex-col px-mobile-gutter pt-[calc(var(--spacing-header)+var(--spacing-xl)+env(safe-area-inset-top))]"
        :class="
          showBottomNavigation
            ? 'pb-[calc(var(--spacing-bottom-nav)+var(--spacing-section)+var(--spacing-xl)+env(safe-area-inset-bottom))]'
            : 'pb-xl'
        "
      >
        <RouterView />
      </main>

      <WardBottomNavigation
        v-if="showBottomNavigation"
        :active="activeNavigation"
        @navigate="handleNavigate"
      />
    </div>
  </div>
</template>
