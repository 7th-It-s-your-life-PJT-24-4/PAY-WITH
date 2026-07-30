import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import {
  issueMockGuardianPairingCode,
  MockPairingError,
  mockActivePairing,
  submitMockWardPairing,
} from '@/mocks/pairing.mock'
import type {
  GuardianPairingCodeResponse,
  PairingErrorCode,
  PairingStatus,
  WardPairingResponse,
} from '@/types/pairing'

export const usePairingStore = defineStore('pairing', () => {
  const status = ref<PairingStatus>('PAIRED')
  const codeResponse = ref<GuardianPairingCodeResponse | null>(null)
  const pairingResult = ref<WardPairingResponse | null>(mockActivePairing)
  const isGuardianMockPaired = ref(false)
  const errorCode = ref<PairingErrorCode | null>(null)
  const isIssuingCode = ref(false)
  const isVerifyingCode = ref(false)

  const isPaired = computed(() => status.value === 'PAIRED')
  const code = computed(() => codeResponse.value?.code ?? '')
  const inviteUrl = computed(() => codeResponse.value?.inviteUrl ?? '')
  const expiresAt = computed(() => codeResponse.value?.expiresAt ?? null)
  const guardian = computed(() =>
    pairingResult.value
      ? {
          id: pairingResult.value.guardId,
          name: pairingResult.value.guardName,
        }
      : null,
  )

  async function issueCode() {
    isIssuingCode.value = true
    errorCode.value = null

    try {
      codeResponse.value = await issueMockGuardianPairingCode()
      status.value = 'CODE_ISSUED'
      return true
    } finally {
      isIssuingCode.value = false
    }
  }

  async function verifyCode(pairingCode: string) {
    isVerifyingCode.value = true
    errorCode.value = null

    try {
      pairingResult.value = await submitMockWardPairing({ pairingCode })
      status.value = 'PAIRED'
      return true
    } catch (error) {
      errorCode.value =
        error instanceof MockPairingError ? error.code : 'PAIRING_002'
      return false
    } finally {
      isVerifyingCode.value = false
    }
  }

  function reset() {
    status.value = 'UNPAIRED'
    codeResponse.value = null
    pairingResult.value = null
    isGuardianMockPaired.value = false
    errorCode.value = null
  }

  function completeGuardianMockPairing() {
    pairingResult.value = mockActivePairing
    status.value = 'PAIRED'
    isGuardianMockPaired.value = true
  }

  return {
    status,
    code,
    inviteUrl,
    expiresAt,
    guardian,
    pairingResult,
    errorCode,
    isIssuingCode,
    isVerifyingCode,
    isPaired,
    isGuardianMockPaired,
    issueCode,
    verifyCode,
    reset,
    completeGuardianMockPairing,
  }
})
