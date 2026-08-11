import { computed, ref } from 'vue'
import { defineStore } from 'pinia'

import { issueGuardianPairingCode, pairWardWithGuardian } from '@/api/pairing'
import { getApiErrorCode, getApiErrorMessage } from '@/api/error'
import type {
  GuardianPairingCode,
  PairingErrorCode,
  PairingStatus,
  WardPairing,
} from '@/schemas/pairing.schema'
import { pairingErrorCodeSchema } from '@/schemas/pairing.schema'

const PAIRING_STORAGE_KEY = 'pay-with:pairing-status'

function getInitialStatus(): PairingStatus {
  if (typeof window === 'undefined') return 'UNPAIRED'
  const stored = window.sessionStorage.getItem(PAIRING_STORAGE_KEY)
  return stored === 'PAIRED' || stored === 'CODE_ISSUED' ? stored : 'UNPAIRED'
}

export const usePairingStore = defineStore('pairing', () => {
  const status = ref<PairingStatus>(getInitialStatus())
  const codeResponse = ref<GuardianPairingCode | null>(null)
  const pairingResult = ref<WardPairing | null>(null)
  const errorCode = ref<PairingErrorCode | null>(null)
  const errorMessage = ref('')
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
    errorMessage.value = ''

    try {
      codeResponse.value = await issueGuardianPairingCode()
      status.value = 'CODE_ISSUED'
      if (typeof window !== 'undefined') {
        window.sessionStorage.setItem(PAIRING_STORAGE_KEY, 'CODE_ISSUED')
      }
      return true
    } catch (error) {
      codeResponse.value = null
      errorMessage.value = await getApiErrorMessage(
        error,
        '인증 코드를 발급하지 못했습니다. 잠시 후 다시 시도해 주세요.',
      )
      return false
    } finally {
      isIssuingCode.value = false
    }
  }

  async function verifyCode(pairingCode: string) {
    isVerifyingCode.value = true
    errorCode.value = null
    errorMessage.value = ''

    try {
      pairingResult.value = await pairWardWithGuardian({ pairingCode })
      codeResponse.value = null
      status.value = 'PAIRED'
      if (typeof window !== 'undefined') {
        window.sessionStorage.setItem(PAIRING_STORAGE_KEY, 'PAIRED')
      }
      return true
    } catch (error) {
      const code = pairingErrorCodeSchema.safeParse(getApiErrorCode(error))
      errorCode.value = code.success ? code.data : null
      errorMessage.value = await getApiErrorMessage(
        error,
        '인증 코드가 유효하지 않거나 만료되었습니다.',
      )
      return false
    } finally {
      isVerifyingCode.value = false
    }
  }

  function markPaired() {
    status.value = 'PAIRED'
    if (typeof window !== 'undefined') {
      window.sessionStorage.setItem(PAIRING_STORAGE_KEY, 'PAIRED')
    }
    errorCode.value = null
    errorMessage.value = ''
  }

  function reset() {
    status.value = 'UNPAIRED'
    if (typeof window !== 'undefined') {
      window.sessionStorage.removeItem(PAIRING_STORAGE_KEY)
    }
    codeResponse.value = null
    pairingResult.value = null
    errorCode.value = null
    errorMessage.value = ''
  }

  return {
    status,
    code,
    inviteUrl,
    expiresAt,
    guardian,
    pairingResult,
    errorCode,
    errorMessage,
    isIssuingCode,
    isVerifyingCode,
    isPaired,
    issueCode,
    markPaired,
    verifyCode,
    reset,
  }
})
