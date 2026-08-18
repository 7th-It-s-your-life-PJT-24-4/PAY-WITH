<script setup lang="ts">
import { AppHeader, Button } from '@pay-with/ui'
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useForm } from 'vee-validate'

import {
  signUpDetailsSchema,
  type SignUpDetailsForm,
} from '@/schemas/sign-up.schema'
import { getApiErrorMessage } from '@/api/error'
import { useCreateUserMutation } from '@/composables/useCreateUserMutation'
import { useLoginMutation } from '@/composables/useLoginMutation'
import { getPostSignUpPath } from '@/router/auth-navigation'
import SignUpAvatarPickerModal from '@/pages/auth/sign-up/details/-components/SignUpAvatarPickerModal.vue'
import SignUpBasicInfoSection from '@/pages/auth/sign-up/details/-components/SignUpBasicInfoSection.vue'
import SignUpPasswordSection from '@/pages/auth/sign-up/details/-components/SignUpPasswordSection.vue'
import SignUpPhoneVerificationSection from '@/pages/auth/sign-up/details/-components/SignUpPhoneVerificationSection.vue'
import SignUpProfileSection from '@/pages/auth/sign-up/details/-components/SignUpProfileSection.vue'
import SignUpTermsSection from '@/pages/auth/sign-up/details/-components/SignUpTermsSection.vue'
import { useSignUpPhoneVerification } from '@/pages/auth/sign-up/details/-composables/useSignUpPhoneVerification'
import { formatBirthDate } from '@/pages/auth/sign-up/details/-utils/sign-up-details-format'
import { usePairingStore } from '@/stores/pairing.store'
import { useSignUpStore } from '@/stores/sign-up.store'

const router = useRouter()
const signUpStore = useSignUpStore()
const pairingStore = usePairingStore()
const isAvatarModalOpen = ref(false)
const formError = ref('')
let signUpDraftTimer: ReturnType<typeof globalThis.setTimeout> | undefined
let shouldPersistSignUpDraft = true
const SIGN_UP_DRAFT_SAVE_DELAY = 300
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

const {
  codeRequested: phoneCodeRequested,
  codeTimerLabel: phoneCodeTimerLabel,
  confirmCode: confirmPhoneCode,
  phoneRequestError,
  requestCode: requestPhoneCode,
  sendCodeMutation: sendPhoneCodeMutation,
  updatePhoneNumber,
  updateVerificationCode: updatePhoneVerificationCode,
  verificationCode: phoneVerificationCode,
  verificationMessage: phoneVerificationMessage,
  verificationToken,
  verifyCodeMutation: verifyPhoneCodeMutation,
} = useSignUpPhoneVerification({
  phoneNumber,
  formError,
  setPhoneNumberError: (message) => setFieldError('phoneNumber', message),
})

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

function updateBirthDate(value: string) {
  birthDate.value = formatBirthDate(value)
}

function updateGender(value: '남' | '여' | undefined) {
  if (value) gender.value = value
}

onBeforeUnmount(() => {
  flushSignUpDraftSave()
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
    await router.replace(getPostSignUpPath(signUpStore.role))
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
