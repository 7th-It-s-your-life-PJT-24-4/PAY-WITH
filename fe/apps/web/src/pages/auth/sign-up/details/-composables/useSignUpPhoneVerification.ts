import { useMutation } from '@tanstack/vue-query'
import { computed, onBeforeUnmount, ref, type Ref } from 'vue'

import { sendPhoneCode, verifyPhoneCode } from '@/api/auth'
import { getApiErrorMessage } from '@/api/error'
import {
  digitsOnly,
  formatPhoneNumber,
} from '@/pages/auth/sign-up/details/-utils/sign-up-details-format'
import {
  phoneCodeRequestSchema,
  phoneVerifyRequestSchema,
} from '@/schemas/auth.schema'
import { useSignUpStore } from '@/stores/sign-up.store'

interface SignUpPhoneVerificationOptions {
  phoneNumber: Ref<string>
  formError: Ref<string>
  setPhoneNumberError: (message?: string) => void
}

export function useSignUpPhoneVerification({
  phoneNumber,
  formError,
  setPhoneNumberError,
}: SignUpPhoneVerificationOptions) {
  const signUpStore = useSignUpStore()
  const phoneRequestError = ref('')
  const verificationCode = ref('')
  const verificationMessage = ref('')
  const codeRequested = ref(false)
  const codeExpiresAt = ref<number | null>(null)
  const remainingCodeSeconds = ref(0)
  const savedVerification = signUpStore.phoneVerification
  const verificationToken = ref<string | null>(
    savedVerification?.phone === digitsOnly(phoneNumber.value, 11)
      ? savedVerification.verificationToken
      : null,
  )
  const sendCodeMutation = useMutation({ mutationFn: sendPhoneCode })
  const verifyCodeMutation = useMutation({ mutationFn: verifyPhoneCode })
  let codeTimer: ReturnType<typeof globalThis.setInterval> | undefined

  function clearCodeTimer() {
    if (codeTimer) globalThis.clearInterval(codeTimer)
    codeTimer = undefined
    codeExpiresAt.value = null
    remainingCodeSeconds.value = 0
  }

  function updateCodeTimer() {
    if (!codeExpiresAt.value) return

    remainingCodeSeconds.value = Math.max(
      0,
      Math.ceil((codeExpiresAt.value - Date.now()) / 1000),
    )

    if (remainingCodeSeconds.value === 0) {
      clearCodeTimer()
      if (!verificationToken.value) {
        verificationMessage.value =
          '인증 시간이 만료되었습니다. 인증번호를 재요청해 주세요.'
      }
    }
  }

  function startCodeTimer(expireIn: number) {
    clearCodeTimer()
    codeExpiresAt.value = Date.now() + expireIn * 1000
    updateCodeTimer()
    codeTimer = globalThis.setInterval(updateCodeTimer, 1000)
  }

  function clearVerification() {
    verificationToken.value = null
    signUpStore.clearPhoneVerification()
    verificationCode.value = ''
    verificationMessage.value = ''
    phoneRequestError.value = ''
    codeRequested.value = false
    clearCodeTimer()
  }

  function updatePhoneNumber(value: string) {
    const formattedPhoneNumber = formatPhoneNumber(value)
    if (phoneNumber.value !== formattedPhoneNumber) clearVerification()
    phoneNumber.value = formattedPhoneNumber
  }

  function updateVerificationCode(value: string) {
    verificationCode.value = digitsOnly(value, 6)
  }

  async function requestCode() {
    formError.value = ''
    phoneRequestError.value = ''
    verificationMessage.value = ''
    verificationToken.value = null
    signUpStore.clearPhoneVerification()

    const result = phoneCodeRequestSchema.safeParse({
      phone: phoneNumber.value,
      purpose: 'SIGNUP',
    })
    if (!result.success) {
      const message =
        result.error.issues[0]?.message ?? '휴대폰 번호를 확인해 주세요.'
      setPhoneNumberError(message)
      phoneRequestError.value = message
      return
    }

    try {
      const response = await sendCodeMutation.mutateAsync(result.data)
      codeRequested.value = true
      verificationCode.value = ''
      startCodeTimer(response.expireIn)
      verificationMessage.value =
        '인증번호를 발송했어요. 제한 시간 안에 입력해 주세요.'
    } catch (error) {
      phoneRequestError.value = await getApiErrorMessage(
        error,
        '인증번호 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.',
      )
    }
  }

  async function confirmCode() {
    formError.value = ''
    const result = phoneVerifyRequestSchema.safeParse({
      phone: phoneNumber.value,
      code: verificationCode.value,
    })
    if (!result.success) {
      formError.value =
        result.error.issues[0]?.message ?? '인증번호를 확인해 주세요.'
      return
    }

    if (remainingCodeSeconds.value === 0) {
      formError.value =
        '인증 시간이 만료되었습니다. 인증번호를 재요청해 주세요.'
      return
    }

    try {
      const response = await verifyCodeMutation.mutateAsync(result.data)
      verificationToken.value = response.verificationToken
      signUpStore.setPhoneVerification({
        phone: result.data.phone,
        verificationToken: response.verificationToken,
      })
      clearCodeTimer()
      verificationMessage.value = '인증 완료했어요.'
      phoneRequestError.value = ''
      setPhoneNumberError()
    } catch (error) {
      formError.value = await getApiErrorMessage(
        error,
        '인증번호를 다시 확인해 주세요.',
      )
    }
  }

  const codeTimerLabel = computed(() => {
    const minutes = Math.floor(remainingCodeSeconds.value / 60)
    const seconds = String(remainingCodeSeconds.value % 60).padStart(2, '0')

    return `${minutes}:${seconds}`
  })

  onBeforeUnmount(clearCodeTimer)

  return {
    codeRequested,
    codeTimerLabel,
    confirmCode,
    phoneRequestError,
    requestCode,
    sendCodeMutation,
    updatePhoneNumber,
    updateVerificationCode,
    verificationCode,
    verificationMessage,
    verificationToken,
    verifyCodeMutation,
  }
}
