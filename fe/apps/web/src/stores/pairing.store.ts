import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { mockPairedGuardian, mockPairingCode } from '@/mocks/pairing.mock'
import type { PairedGuardian, PairingStatus } from '@/types/pairing'

const pairingCodeLifetime = 5 * 60 * 1000

export const usePairingStore = defineStore('pairing', () => {
  const status = ref<PairingStatus>('UNPAIRED')
  const code = ref('')
  const expiresAt = ref<number | null>(null)
  const guardian = ref<PairedGuardian | null>(null)

  const isPaired = computed(() => status.value === 'PAIRED')

  function issueCode() {
    code.value = mockPairingCode
    expiresAt.value = Date.now() + pairingCodeLifetime
    status.value = 'CODE_ISSUED'
  }

  function verifyCode(value: string) {
    if (value !== mockPairingCode) return false

    guardian.value = mockPairedGuardian
    status.value = 'PAIRED'
    return true
  }

  function reset() {
    status.value = 'UNPAIRED'
    code.value = ''
    expiresAt.value = null
    guardian.value = null
  }

  return {
    status,
    code,
    expiresAt,
    guardian,
    isPaired,
    issueCode,
    verifyCode,
    reset,
  }
})
