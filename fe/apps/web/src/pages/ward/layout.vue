<script setup lang="ts">
import { AppHeader } from '@pay-with/ui'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import WardBottomNavigation from '@/pages/ward/-components/WardBottomNavigation.vue'
import ForegroundPushNotification from '@/components/notifications/ForegroundPushNotification.vue'
import { usePairingStore } from '@/stores/pairing.store'

type WardNavigationValue = 'transfer' | 'home' | 'payment'

const router = useRouter()
const route = useRoute()
const pairingStore = usePairingStore()

const headerTitle = computed(() => String(route.meta.title ?? 'PayWith'))
const showBack = computed(() => route.meta.showBack !== false)
const showBottomNavigation = computed(
  () => route.meta.showBottomNavigation !== false,
)
const activeNavigation = computed<WardNavigationValue | ''>(() => {
  const value = route.meta.activeNavigation
  if (value === 'transfer' || value === 'home' || value === 'payment')
    return value
  return ''
})

function goBack() {
  const backRouteName = route.meta.backRouteName
  if (typeof backRouteName === 'string') {
    router.replace({ name: backRouteName })
    return
  }
  router.back()
}

function handleNavigate(value: string) {
  if (!pairingStore.isPaired && (value === 'transfer' || value === 'payment')) {
    return
  }
  if (value === 'transfer') router.push({ name: 'ward-transfer' })
  if (value === 'home') router.push({ name: 'ward-home' })
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
        <AppHeader
          :title="headerTitle"
          :show-back="showBack"
          show-profile
          variant="primary"
          @back="goBack"
          @profile="router.push({ name: 'ward-my' })"
        />
      </div>

      <main
        class="flex flex-1 flex-col px-mobile-gutter pt-[calc(var(--spacing-header)+var(--spacing-xl)+env(safe-area-inset-top))]"
        :class="
          showBottomNavigation
            ? 'pb-[calc(100px+var(--spacing-section)+env(safe-area-inset-bottom))]'
            : 'pb-xl'
        "
      >
        <ForegroundPushNotification />
        <RouterView />
      </main>

      <WardBottomNavigation
        v-if="showBottomNavigation"
        :active="activeNavigation"
        :is-paired="pairingStore.isPaired"
        @navigate="handleNavigate"
      />
    </div>
  </div>
</template>
