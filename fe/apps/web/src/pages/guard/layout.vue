<script setup lang="ts">
import { computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import GuardBottomNavigation, {
  type GuardNavigationValue,
} from '@/pages/guard/-components/GuardBottomNavigation.vue'
import {
  parsePositiveRouteId,
  withGuardWardId,
} from '@/pages/guard/-utils/guard-route'
import { useGuardStore } from '@/stores/guard.store'

const route = useRoute()
const router = useRouter()
const guardStore = useGuardStore()

const routeWardId = computed(() => parsePositiveRouteId(route.query.wardId))
const activeWardId = computed(
  () => routeWardId.value ?? guardStore.activeWardId,
)

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
  const query = withGuardWardId({}, activeWardId.value)

  if (value === 'home') router.push({ name: 'guard-home', query })
  if (value === 'charge') router.push({ name: 'guard-charge', query })
  if (value === 'history') router.push({ name: 'guard-history', query })
  if (value === 'my') router.push({ name: 'guard-my' })
}

watch(
  routeWardId,
  (wardId) => {
    if (wardId !== null) guardStore.selectWard(wardId)
  },
  { immediate: true },
)
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
