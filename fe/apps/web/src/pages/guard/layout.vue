<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import GuardBottomNavigation, {
  type GuardNavigationValue,
} from '@/pages/guard/-components/GuardBottomNavigation.vue'

const route = useRoute()
const router = useRouter()

const showBottomNavigation = computed(
  () => route.meta.showBottomNavigation !== false,
)
const activeNavigation = computed<GuardNavigationValue>(() => {
  const value = route.meta.activeNavigation
  if (
    value === 'charge' ||
    value === 'history' ||
    value === 'my' ||
    value === 'home'
  ) {
    return value
  }

  return 'home'
})

function handleNavigate(value: GuardNavigationValue) {
  if (value === 'home') router.push({ name: 'guard-home' })
  if (value === 'charge') router.push({ name: 'guard-charge' })
  if (value === 'history') router.push({ name: 'guard-history' })
  if (value === 'my') router.push({ name: 'guard-my' })
}
</script>

<template>
  <div class="min-h-screen bg-gray-900 px-0 sm:px-md">
    <div
      class="mx-auto min-h-screen w-full max-w-[390px] bg-white text-body shadow-card"
    >
      <RouterView />

      <GuardBottomNavigation
        v-if="showBottomNavigation"
        :active="activeNavigation"
        @navigate="handleNavigate"
      />
    </div>
  </div>
</template>
