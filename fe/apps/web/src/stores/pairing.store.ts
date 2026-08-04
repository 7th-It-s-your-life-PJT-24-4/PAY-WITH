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

export const usePairingStore = defineStore('pairing', () => {
  const status = ref<PairingStatus>('UNPAIRED')
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

  function reset() {
    status.value = 'UNPAIRED'
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
    verifyCode,
    reset,
  }
})
