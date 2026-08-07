<script setup lang="ts">
import { AppHeader, Button } from '@pay-with/ui'
import { useMutation } from '@tanstack/vue-query'
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useForm } from 'vee-validate'

import {
  signUpDetailsSchema,
  type SignUpDetailsForm,
} from '@/schemas/sign-up.schema'
import { getApiErrorMessage } from '@/api/error'
import { sendPhoneCode, verifyPhoneCode } from '@/api/auth'
import {
  phoneCodeRequestSchema,
  phoneVerifyRequestSchema,
} from '@/schemas/auth.schema'
import { useCreateUserMutation } from '@/composables/useCreateUserMutation'
import { useLoginMutation } from '@/composables/useLoginMutation'
import SignUpAvatarPickerModal from '@/pages/auth/sign-up/details/-components/SignUpAvatarPickerModal.vue'
import SignUpBasicInfoSection from '@/pages/auth/sign-up/details/-components/SignUpBasicInfoSection.vue'
import SignUpPasswordSection from '@/pages/auth/sign-up/details/-components/SignUpPasswordSection.vue'
import SignUpPhoneVerificationSection from '@/pages/auth/sign-up/details/-components/SignUpPhoneVerificationSection.vue'
import SignUpProfileSection from '@/pages/auth/sign-up/details/-components/SignUpProfileSection.vue'
import SignUpTermsSection from '@/pages/auth/sign-up/details/-components/SignUpTermsSection.vue'
import { usePairingStore } from '@/stores/pairing.store'
import { useSignUpStore } from '@/stores/sign-up.store'

const router = useRouter()
const signUpStore = useSignUpStore()
const pairingStore = usePairingStore()
const isAvatarModalOpen = ref(false)
const formError = ref('')
const phoneRequestError = ref('')
const phoneVerificationCode = ref('')
const phoneVerificationMessage = ref('')
const phoneCodeRequested = ref(false)
const phoneCodeExpiresAt = ref<number | null>(null)
const remainingPhoneCodeSeconds = ref(0)
let phoneCodeTimer: ReturnType<typeof globalThis.setInterval> | undefined
let signUpDraftTimer: ReturnType<typeof globalThis.setTimeout> | undefined
let shouldPersistSignUpDraft = true
const SIGN_UP_DRAFT_SAVE_DELAY = 300
const sendPhoneCodeMutation = useMutation({ mutationFn: sendPhoneCode })
const verifyPhoneCodeMutation = useMutation({ mutationFn: verifyPhoneCode })
const createUserMutation = useCreateUserMutation()
const loginMutation = useLoginMutation()

const initialDetails = signUpStore.draft ??
  signUpStore.details ?? {
    fullName: '',
    phoneNumber: '',
    birthDate: '',
    gender: undefined,
    avatarId: null,
    loginPassword: '',
    paymentPassword: '',
    serviceTerms: false,
    privacyTerms: false,
    identifierTerms: false,
  }
const savedPhoneVerification = signUpStore.phoneVerification
const verificationToken = ref<string | null>(
  savedPhoneVerification?.phone ===
    initialDetails.phoneNumber.replace(/\D/g, '')
    ? savedPhoneVerification.verificationToken
    : null,
)

const { defineField, errors, setErrors, setFieldError } =
  useForm<SignUpDetailsForm>({
    initialValues: {
      ...initialDetails,
      birthDate: formatBirthDate(initialDetails.birthDate),
    },
  })

const [fullName] = defineField('fullName')
const [phoneNumber] = defineField('phoneNumber')
const [birthDate] = defineField('birthDate')
const [gender] = defineField('gender')
const [avatarId] = defineField('avatarId')
const [loginPassword] = defineField('loginPassword')
const [paymentPassword] = defineField('paymentPassword')
const [serviceTerms] = defineField('serviceTerms')
const [privacyTerms] = defineField('privacyTerms')
const [identifierTerms] = defineField('identifierTerms')

function saveSignUpDraft() {
  if (!shouldPersistSignUpDraft) return

  signUpStore.setDraft({
    fullName: fullName.value,
    phoneNumber: phoneNumber.value,
    birthDate: birthDate.value,
    gender: gender.value,
    avatarId: avatarId.value,
    loginPassword: loginPassword.value,
    paymentPassword: paymentPassword.value,
    serviceTerms: serviceTerms.value,
    privacyTerms: privacyTerms.value,
    identifierTerms: identifierTerms.value,
  })
}

function clearSignUpDraftTimer() {
  if (!signUpDraftTimer) return

  globalThis.clearTimeout(signUpDraftTimer)
  signUpDraftTimer = undefined
}

function scheduleSignUpDraftSave() {
  clearSignUpDraftTimer()
  signUpDraftTimer = globalThis.setTimeout(() => {
    signUpDraftTimer = undefined
    saveSignUpDraft()
  }, SIGN_UP_DRAFT_SAVE_DELAY)
}

function flushSignUpDraftSave() {
  clearSignUpDraftTimer()
  saveSignUpDraft()
}

function clearFieldErrorsIfValid() {
  const currentValues: Record<keyof SignUpDetailsForm, unknown> = {
    fullName: fullName.value,
    phoneNumber: phoneNumber.value,
    birthDate: birthDate.value,
    gender: gender.value,
    avatarId: avatarId.value,
    loginPassword: loginPassword.value,
    paymentPassword: paymentPassword.value,
    serviceTerms: serviceTerms.value,
    privacyTerms: privacyTerms.value,
    identifierTerms: identifierTerms.value,
  }

  for (const [key, schema] of Object.entries(signUpDetailsSchema.shape)) {
    const fieldKey = key as keyof SignUpDetailsForm
    if (errors.value[fieldKey]) {
      const fieldResult = schema.safeParse(currentValues[fieldKey])
      if (fieldResult.success) {
        setFieldError(fieldKey, undefined)
      }
    }
  }
}

watch(
  [
    fullName,
    phoneNumber,
    birthDate,
    gender,
    avatarId,
    loginPassword,
    paymentPassword,
    serviceTerms,
    privacyTerms,
    identifierTerms,
  ],
  () => {
    scheduleSignUpDraftSave()
    clearFieldErrorsIfValid()
  },
)

const termsError = computed(() => {
  const tError =
    errors.value.serviceTerms ||
    errors.value.privacyTerms ||
    errors.value.identifierTerms
  return tError ? '모든 필수 약관에 동의해 주세요.' : undefined
})

async function scrollToFirstError() {
  await nextTick()
  const errorElement = globalThis.document.querySelector<HTMLElement>(
    '[aria-invalid="true"], .text-error',
  )
  if (errorElement) {
    errorElement.scrollIntoView({ behavior: 'smooth', block: 'center' })
    if ('focus' in errorElement && typeof errorElement.focus === 'function') {
      errorElement.focus()
    }
  }
}

function formatPhoneNumber(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 11)

  if (digits.length <= 3) return digits
  if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`

  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
}

function formatBirthDate(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 8)

  if (digits.length <= 4) return digits
  if (digits.length <= 6) return `${digits.slice(0, 4)}.${digits.slice(4)}`

  return `${digits.slice(0, 4)}.${digits.slice(4, 6)}.${digits.slice(6)}`
}

function updateBirthDate(value: string) {
  birthDate.value = formatBirthDate(value)
}

function updateGender(value: '남' | '여' | undefined) {
  if (value) gender.value = value
}

function updatePhoneVerificationCode(value: string) {
  phoneVerificationCode.value = value.replace(/\D/g, '').slice(0, 6)
}

function clearPhoneCodeTimer() {
  if (phoneCodeTimer) globalThis.clearInterval(phoneCodeTimer)
  phoneCodeTimer = undefined
  phoneCodeExpiresAt.value = null
  remainingPhoneCodeSeconds.value = 0
}

function updatePhoneCodeTimer() {
  if (!phoneCodeExpiresAt.value) return

  remainingPhoneCodeSeconds.value = Math.max(
    0,
    Math.ceil((phoneCodeExpiresAt.value - Date.now()) / 1000),
  )

  if (remainingPhoneCodeSeconds.value === 0) {
    clearPhoneCodeTimer()
    if (!verificationToken.value) {
      phoneVerificationMessage.value =
        '인증 시간이 만료되었습니다. 인증번호를 재요청해 주세요.'
    }
  }
}

function startPhoneCodeTimer(expireIn: number) {
  clearPhoneCodeTimer()
  phoneCodeExpiresAt.value = Date.now() + expireIn * 1000
  updatePhoneCodeTimer()
  phoneCodeTimer = globalThis.setInterval(updatePhoneCodeTimer, 1000)
}

const phoneCodeTimerLabel = computed(() => {
  const minutes = Math.floor(remainingPhoneCodeSeconds.value / 60)
  const seconds = String(remainingPhoneCodeSeconds.value % 60).padStart(2, '0')

  return `${minutes}:${seconds}`
})

function updatePhoneNumber(value: string) {
  if (phoneNumber.value !== formatPhoneNumber(value)) {
    verificationToken.value = null
    signUpStore.clearPhoneVerification()
    phoneVerificationCode.value = ''
    phoneVerificationMessage.value = ''
    phoneRequestError.value = ''
    phoneCodeRequested.value = false
    clearPhoneCodeTimer()
  }
  phoneNumber.value = formatPhoneNumber(value)
}

async function requestPhoneCode() {
  formError.value = ''
  phoneRequestError.value = ''
  phoneVerificationMessage.value = ''
  verificationToken.value = null
  signUpStore.clearPhoneVerification()

  const result = phoneCodeRequestSchema.safeParse({
    phone: phoneNumber.value,
    purpose: 'SIGNUP',
  })
  if (!result.success) {
    const message =
      result.error.issues[0]?.message ?? '휴대폰 번호를 확인해 주세요.'
    setErrors({ phoneNumber: message })
    phoneRequestError.value = message
    return
  }

  try {
    const response = await sendPhoneCodeMutation.mutateAsync(result.data)
    phoneCodeRequested.value = true
    phoneVerificationCode.value = ''
    startPhoneCodeTimer(response.expireIn)
    phoneVerificationMessage.value =
      '인증번호를 발송했어요. 제한 시간 안에 입력해 주세요.'
  } catch (error) {
    phoneRequestError.value = await getApiErrorMessage(
      error,
      '인증번호 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.',
    )
  }
}

async function confirmPhoneCode() {
  formError.value = ''
  const result = phoneVerifyRequestSchema.safeParse({
    phone: phoneNumber.value,
    code: phoneVerificationCode.value,
  })
  if (!result.success) {
    formError.value =
      result.error.issues[0]?.message ?? '인증번호를 확인해 주세요.'
    return
  }

  if (remainingPhoneCodeSeconds.value === 0) {
    formError.value = '인증 시간이 만료되었습니다. 인증번호를 재요청해 주세요.'
    return
  }

  try {
    const response = await verifyPhoneCodeMutation.mutateAsync(result.data)
    verificationToken.value = response.verificationToken
    signUpStore.setPhoneVerification({
      phone: result.data.phone,
      verificationToken: response.verificationToken,
    })
    clearPhoneCodeTimer()
    phoneVerificationMessage.value = '인증 완료했어요.'
    phoneRequestError.value = ''
    setFieldError('phoneNumber', undefined)
  } catch (error) {
    formError.value = await getApiErrorMessage(
      error,
      '인증번호를 다시 확인해 주세요.',
    )
  }
}

onBeforeUnmount(() => {
  flushSignUpDraftSave()
  clearPhoneCodeTimer()
})

async function submitSignUp() {
  formError.value = ''
  phoneRequestError.value = ''
  setErrors({})

  const result = signUpDetailsSchema.safeParse({
    fullName: fullName.value,
    phoneNumber: phoneNumber.value,
    birthDate: birthDate.value,
    gender: gender.value,
    avatarId: avatarId.value,
    loginPassword: loginPassword.value,
    paymentPassword: paymentPassword.value,
    serviceTerms: serviceTerms.value,
    privacyTerms: privacyTerms.value,
    identifierTerms: identifierTerms.value,
  })

  if (!result.success) {
    const fieldErrors = result.error.flatten().fieldErrors
    setErrors(
      Object.fromEntries(
        Object.entries(fieldErrors).map(([key, messages]) => [
          key,
          messages?.[0],
        ]),
      ),
    )
    formError.value = '입력한 정보를 다시 확인해 주세요.'
    await scrollToFirstError()
    return
  }

  if (!verificationToken.value || !signUpStore.role) {
    if (verificationToken.value === null) {
      setErrors({ phoneNumber: '휴대폰 인증을 완료해 주세요.' })
      formError.value = '휴대폰 인증을 완료해 주세요.'
    } else {
      formError.value = '가입 유형을 다시 선택해 주세요.'
    }
    await scrollToFirstError()
    return
  }

  const role = signUpStore.role === 'senior' ? 'WARD' : 'GUARD'

  let isUserCreated = false

  try {
    await createUserMutation.mutateAsync({
      role,
      phone: result.data.phoneNumber,
      password: result.data.loginPassword,
      name: result.data.fullName,
      birthDate: result.data.birthDate,
      gender: result.data.gender,
      avatarId: result.data.avatarId,
      paymentPassword: result.data.paymentPassword,
      verificationToken: verificationToken.value,
    })
    isUserCreated = true
    await loginMutation.mutateAsync({
      phone: result.data.phoneNumber,
      password: result.data.loginPassword,
    })

    shouldPersistSignUpDraft = false
    clearSignUpDraftTimer()
    signUpStore.setDetails(result.data)
    signUpStore.clearDraft()
    signUpStore.clearPhoneVerification()
    formError.value = ''
    if (signUpStore.role === 'senior') pairingStore.reset()
    await router.replace(signUpStore.role === 'guardian' ? '/guard' : '/ward')
  } catch (error) {
    if (isUserCreated) {
      shouldPersistSignUpDraft = false
      clearSignUpDraftTimer()
      signUpStore.clearDraft()
      signUpStore.clearPhoneVerification()
      await router.replace({
        name: 'auth-sign-in',
        query: { signup: 'completed' },
      })
      return
    }

    formError.value = await getApiErrorMessage(
      error,
      '회원가입에 실패했습니다. 입력 정보를 다시 확인해 주세요.',
    )
  }
}

function openAvatarModal() {
  isAvatarModalOpen.value = true
}
</script>

<template>
  <main class="mx-auto min-h-screen w-full max-w-[390px] bg-surface">
    <AppHeader title="회원가입" show-back @back="router.back()" />

    <form class="pb-[128px]" @submit.prevent="submitSignUp">
      <section class="flex flex-col items-center px-mobile-gutter pt-xxl">
        <div class="mb-xs flex gap-xs" aria-hidden="true">
          <span class="size-xs rounded-full bg-border" />
          <span class="size-xs rounded-full bg-primary-300" />
        </div>
        <p class="type-caption text-body-muted">2 / 2 단계</p>
      </section>

      <div class="flex flex-col gap-xxl px-mobile-gutter pt-section">
        <section>
          <h1 class="type-h1 text-body">환영합니다</h1>
          <p class="type-h4 mt-xs text-body-muted">
            안전한 결제를 위해 정보를 입력해주세요.
          </p>
        </section>

        <SignUpProfileSection :avatar-id="avatarId" @select="openAvatarModal" />

        <SignUpBasicInfoSection
          :birth-date="birthDate"
          :birth-date-error="errors.birthDate"
          :full-name="fullName"
          :full-name-error="errors.fullName"
          :gender="gender"
          :gender-error="errors.gender"
          @update:birth-date="updateBirthDate"
          @update:full-name="fullName = $event"
          @update:gender="updateGender"
        />

        <SignUpPhoneVerificationSection
          :code-requested="phoneCodeRequested"
          :code-timer-label="phoneCodeTimerLabel"
          :is-sending="sendPhoneCodeMutation.isPending.value"
          :is-verifying="verifyPhoneCodeMutation.isPending.value"
          :phone-number="phoneNumber"
          :phone-number-error="errors.phoneNumber"
          :phone-request-error="phoneRequestError"
          :verification-code="phoneVerificationCode"
          :verification-message="phoneVerificationMessage"
          :verification-token="verificationToken"
          @confirm="confirmPhoneCode"
          @request="requestPhoneCode"
          @update:phone-number="updatePhoneNumber"
          @update:verification-code="updatePhoneVerificationCode"
        />

        <SignUpPasswordSection
          :login-password="loginPassword"
          :login-password-error="errors.loginPassword"
          :payment-password="paymentPassword"
          :payment-password-error="errors.paymentPassword"
          @update:login-password="loginPassword = $event"
          @update:payment-password="paymentPassword = $event"
        />

        <SignUpTermsSection
          :identifier-terms="identifierTerms"
          :privacy-terms="privacyTerms"
          :service-terms="serviceTerms"
          :terms-error="termsError"
          @before-navigate="flushSignUpDraftSave"
          @update:identifier-terms="identifierTerms = $event"
          @update:privacy-terms="privacyTerms = $event"
          @update:service-terms="serviceTerms = $event"
        />
        <p
          v-if="formError"
          class="type-caption -mt-lg text-center text-error"
          role="alert"
        >
          {{ formError }}
        </p>
      </div>

      <div
        class="fixed inset-x-0 bottom-0 z-10 mx-auto w-full max-w-[390px] bg-gradient-to-t from-surface via-surface to-transparent px-mobile-gutter pb-lg pt-section"
      >
        <Button
          class="w-full shadow-[0_10px_15px_-3px_rgb(0_0_0/10%),0_4px_6px_-4px_rgb(0_0_0/10%)]"
          :label="
            createUserMutation.isPending.value ? '가입 중...' : '가입하기'
          "
          type="submit"
          :disabled="
            createUserMutation.isPending.value || loginMutation.isPending.value
          "
        />
      </div>
    </form>

    <SignUpAvatarPickerModal
      v-model="isAvatarModalOpen"
      :avatar-id="avatarId"
      @select="avatarId = $event"
    />
  </main>
</template>
