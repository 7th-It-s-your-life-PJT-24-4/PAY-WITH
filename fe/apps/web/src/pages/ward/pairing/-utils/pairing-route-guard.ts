import type { NavigationGuard } from 'vue-router'

import { usePairingStore } from '@/stores/pairing.store'

export const requireCompletedPairing: NavigationGuard = () => {
  const pairingStore = usePairingStore()

  if (!pairingStore.isPaired) {
    return { name: 'ward-home', replace: true }
  }

  return true
}
