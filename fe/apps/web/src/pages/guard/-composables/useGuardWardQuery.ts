import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useGuardStore } from '@/stores/guard.store'

export function parseWardIdQuery(value: unknown): number | null {
  const normalized = Array.isArray(value) ? value[0] : value
  if (typeof normalized !== 'string' || !/^\d+$/.test(normalized)) return null

  const wardId = Number(normalized)
  return Number.isSafeInteger(wardId) && wardId > 0 ? wardId : null
}

export function useGuardWardQuery() {
  const route = useRoute()
  const router = useRouter()
  const guardStore = useGuardStore()
  const selectedWardId = ref<number | null>(
    parseWardIdQuery(route.query.wardId) ?? guardStore.activeWardId,
  )

  watch(
    () => route.query.wardId,
    (queryWardId) => {
      const wardId = parseWardIdQuery(queryWardId)
      if (wardId === null) return

      selectedWardId.value = wardId
      guardStore.selectWard(wardId)
    },
    { immediate: true },
  )

  function selectWard(wardId: number) {
    if (!Number.isSafeInteger(wardId) || wardId <= 0) return

    selectedWardId.value = wardId
    guardStore.selectWard(wardId)

    if (parseWardIdQuery(route.query.wardId) === wardId) return

    void router.replace({
      query: {
        ...route.query,
        wardId: String(wardId),
      },
    })
  }

  return {
    selectedWardId,
    selectWard,
  }
}
