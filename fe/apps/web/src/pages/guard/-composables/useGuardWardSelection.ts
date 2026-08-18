import { computed, watch, type WatchSource } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  parsePositiveRouteId,
  withGuardWardId,
} from '@/pages/guard/-utils/guard-route'
import { useGuardStore } from '@/stores/guard.store'

export function useGuardWardSelection() {
  const route = useRoute()
  const router = useRouter()
  const guardStore = useGuardStore()
  const routeWardId = computed(() => parsePositiveRouteId(route.query.wardId))
  const activeWardId = computed(
    () => routeWardId.value ?? guardStore.activeWardId,
  )

  watch(
    routeWardId,
    (wardId) => {
      if (wardId !== null) guardStore.selectWard(wardId)
    },
    { immediate: true },
  )

  async function updateWardSelection(wardId: number) {
    guardStore.selectWard(wardId)
    if (routeWardId.value === wardId) return

    await router.replace({
      query: withGuardWardId(route.query, wardId),
    })
  }

  async function selectWard(value: unknown) {
    const wardId = parsePositiveRouteId(value)
    if (wardId === null) return false

    await updateWardSelection(wardId)
    return true
  }

  function syncSelectedWard(source: WatchSource<number | null | undefined>) {
    watch(
      source,
      (wardId) => {
        if (wardId) void updateWardSelection(wardId)
      },
      { immediate: true },
    )
  }

  async function clearWardSelection() {
    guardStore.clearWardSelection()
    if (!('wardId' in route.query)) return

    await router.replace({
      query: withGuardWardId(route.query, null),
    })
  }

  return {
    activeWardId,
    clearWardSelection,
    routeWardId,
    selectWard,
    syncSelectedWard,
  }
}
