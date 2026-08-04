<script setup lang="ts">
import { Check, ChevronDown, ChevronRight, Eye, EyeOff } from '@lucide/vue'
import { AppHeader, Button, Input } from '@pay-with/ui'
import { useMutation } from '@tanstack/vue-query'
import {
  CheckboxIndicator,
  CheckboxRoot,
  SelectContent,
  SelectItem,
  SelectItemIndicator,
  SelectItemText,
  SelectPortal,
  SelectRoot,
  SelectTrigger,
  SelectValue,
  SelectViewport,
} from 'reka-ui'
import { computed, onBeforeUnmount, ref, useId, watch } from 'vue'
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
import { signUpTerms, type SignUpTermId } from '@/constants/sign-up-terms'
import { useCreateUserMutation } from '@/composables/useCreateUserMutation'
import { useLoginMutation } from '@/composables/useLoginMutation'
import { usePairingStore } from '@/stores/pairing.store'
import { useSignUpStore } from '@/stores/sign-up.store'

const router = useRouter()
const signUpStore = useSignUpStore()
const pairingStore = usePairingStore()
const isPasswordVisible = ref(false)
const formError = ref('')
const phoneVerificationCode = ref('')
const phoneVerificationMessage = ref('')
const phoneCodeRequested = ref(false)
const phoneCodeExpiresAt = ref<number | null>(null)
const remainingPhoneCodeSeconds = ref(0)
let phoneCodeTimer: ReturnType<typeof globalThis.setInterval> | undefined
let signUpDraftTimer: ReturnType<typeof globalThis.setTimeout> | undefined
let shouldPersistSignUpDraft = true
const SIGN_UP_DRAFT_SAVE_DELAY = 300
const paymentPasswordInputIds = Array.from({ length: 6 }, () => useId())
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

const { defineField, errors, setErrors } = useForm<SignUpDetailsForm>({
  initialValues: {
    ...initialDetails,
    birthDate: formatBirthDate(initialDetails.birthDate),
  },
})

const [fullName] = defineField('fullName')
const [phoneNumber] = defineField('phoneNumber')
const [birthDate] = defineField('birthDate')
const [gender] = defineField('gender')
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

watch(
  [
    fullName,
    phoneNumber,
    birthDate,
    gender,
    loginPassword,
    paymentPassword,
    serviceTerms,
    privacyTerms,
    identifierTerms,
  ],
  scheduleSignUpDraftSave,
)

const requiredTermsAgreed = computed(
  () => serviceTerms.value && privacyTerms.value && identifierTerms.value,
)
const allTermsAgreed = computed(() => requiredTermsAgreed.value)
const isReadyToSubmit = computed(() => {
  const result = signUpDetailsSchema.safeParse({
    fullName: fullName.value,
    phoneNumber: phoneNumber.value,
    birthDate: birthDate.value,
    gender: gender.value,
    loginPassword: loginPassword.value,
    paymentPassword: paymentPassword.value,
    serviceTerms: serviceTerms.value,
    privacyTerms: privacyTerms.value,
    identifierTerms: identifierTerms.value,
  })

  return (
    result.success &&
    verificationToken.value !== null &&
    signUpStore.role !== null
  )
})

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
    phoneCodeRequested.value = false
    clearPhoneCodeTimer()
  }
  phoneNumber.value = formatPhoneNumber(value)
}

function inputValueFromEvent(event: unknown) {
  if (typeof event !== 'object' || event === null) return ''

  const target = Reflect.get(event, 'target')
  if (typeof target !== 'object' || target === null) return ''

  const value = Reflect.get(target, 'value')
  return typeof value === 'string' ? value : ''
}

function updatePhoneNumberFromEvent(event: unknown) {
  updatePhoneNumber(inputValueFromEvent(event))
}

function updateBirthDateFromEvent(event: unknown) {
  updateBirthDate(inputValueFromEvent(event))
}

function toggleAllTerms(value: boolean | 'indeterminate') {
  const isChecked = value === true

  serviceTerms.value = isChecked
  privacyTerms.value = isChecked
  identifierTerms.value = isChecked
}

function isTermAgreed(term: SignUpTermId) {
  return {
    serviceTerms: serviceTerms.value,
    privacyTerms: privacyTerms.value,
    identifierTerms: identifierTerms.value,
  }[term]
}

function setTermAgreement(
  term: SignUpTermId,
  value: boolean | 'indeterminate',
) {
  const isChecked = value === true

  if (term === 'serviceTerms') serviceTerms.value = isChecked
  if (term === 'privacyTerms') privacyTerms.value = isChecked
  if (term === 'identifierTerms') identifierTerms.value = isChecked
}

function updatePaymentPassword(index: number, value: string) {
  const digit = value.replace(/\D/g, '').slice(-1)
  const nextValue = paymentPassword.value.split('')

  nextValue[index] = digit
  paymentPassword.value = nextValue.join('').slice(0, 6)

  if (digit && index < 5) {
    globalThis.document
      .getElementById(paymentPasswordInputIds[index + 1])
      ?.focus()
  }
}

function updatePaymentPasswordFromEvent(index: number, event: unknown) {
  updatePaymentPassword(index, inputValueFromEvent(event))
}

function handlePaymentPasswordKeydown(
  index: number,
  event: globalThis.KeyboardEvent,
) {
  if (event.key !== 'Backspace' || index === 0) return

  event.preventDefault()
  if (paymentDigit(index)) updatePaymentPassword(index, '')
  globalThis.document
    .getElementById(paymentPasswordInputIds[index - 1])
    ?.focus()
}

function paymentDigit(index: number) {
  return paymentPassword.value[index] ?? ''
}

async function requestPhoneCode() {
  formError.value = ''
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
    formError.value = message
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
    formError.value = await getApiErrorMessage(
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
  const result = signUpDetailsSchema.safeParse({
    fullName: fullName.value,
    phoneNumber: phoneNumber.value,
    birthDate: birthDate.value,
    gender: gender.value,
    loginPassword: loginPassword.value,
    paymentPassword: paymentPassword.value,
    serviceTerms: serviceTerms.value,
    privacyTerms: privacyTerms.value,
    identifierTerms: identifierTerms.value,
  })

  if (!result.success) {
    const errors = result.error.flatten().fieldErrors
    setErrors(
      Object.fromEntries(
        Object.entries(errors).map(([key, messages]) => [key, messages?.[0]]),
      ),
    )
    formError.value = '입력한 정보를 다시 확인해 주세요.'
    return
  }

  if (!verificationToken.value || !signUpStore.role) {
    formError.value =
      verificationToken.value === null
        ? '휴대폰 인증을 완료해 주세요.'
        : '가입 유형을 다시 선택해 주세요.'
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

        <!--
          프로필 사진은 현재 백엔드 사용자 스키마와 회원가입 API에 저장 필드가 없어
          저장 기능이 구현될 때까지 노출하지 않는다.
        <section>
          <p class="type-h4 text-body">프로필 이미지</p>
          <p class="type-body mt-xs text-body-muted">
            나를 표현할 이미지를 선택해 주세요
          </p>
          <div class="flex flex-col items-center pb-md pt-xl">
            <button
              class="relative flex size-[128px] items-center justify-center rounded-full border-2 border-dashed border-border-strong bg-disabled outline-none focus-visible:ring-2 focus-visible:ring-focus focus-visible:ring-offset-2"
              type="button"
              aria-label="프로필 사진 선택하기"
              @click="selectProfileImage"
            >
              <img
                v-if="profileImageUrl"
                :src="profileImageUrl"
                alt="선택한 프로필 사진"
                class="size-full rounded-full object-cover"
              />
              <Camera
                v-else
                aria-hidden="true"
                class="size-10 text-body-muted"
              />
              <span
                aria-hidden="true"
                class="absolute bottom-0 right-0 flex size-[40px] items-center justify-center rounded-full border-4 border-surface bg-primary-500 text-on-action shadow-card"
              >
                <Plus class="size-lg" :stroke-width="3" />
              </span>
            </button>
            <input
              ref="profileImageInput"
              class="sr-only"
              accept="image/*"
              type="file"
              @change="updateProfileImage"
            />
            <Button
              class="mt-md"
              label="프로필 사진 선택하기"
              pill
              size="small"
              variant="outline-primary"
              type="button"
              @click="selectProfileImage"
            />
          </div>
        </section>
        -->

        <div class="flex flex-col gap-xl">
          <Input
            v-model="fullName"
            :error="errors.fullName"
            :maxlength="30"
            autocomplete="name"
            label="성함 (실명)"
            placeholder="성함을 입력하세요"
          />

          <section class="flex flex-col gap-sm">
            <label class="type-h4 text-body" for="sign-up-birth-date"
              >생년월일</label
            >
            <input
              id="sign-up-birth-date"
              :value="birthDate"
              autocomplete="bday"
              class="h-[52px] w-full rounded-medium border border-border-strong bg-surface-card px-md text-[16px] font-normal leading-[1.2] tracking-[-0.32px] text-body outline-none placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20"
              inputmode="numeric"
              maxlength="10"
              pattern="\d{4}\.\d{2}\.\d{2}"
              placeholder="YYYY.MM.DD"
              type="text"
              :aria-invalid="errors.birthDate ? 'true' : undefined"
              :aria-describedby="
                errors.birthDate ? 'sign-up-birth-date-error' : undefined
              "
              @input="updateBirthDateFromEvent($event)"
            />
            <p
              v-if="errors.birthDate"
              id="sign-up-birth-date-error"
              class="type-caption text-error"
            >
              {{ errors.birthDate }}
            </p>
          </section>

          <section class="flex flex-col gap-sm">
            <label id="sign-up-gender-label" class="type-h4 text-body"
              >성별</label
            >
            <SelectRoot v-model="gender">
              <SelectTrigger
                aria-labelledby="sign-up-gender-label"
                :aria-invalid="errors.gender ? 'true' : undefined"
                class="group flex h-[52px] w-full items-center gap-sm rounded-medium border border-border-strong bg-surface-card px-md text-left text-[14px] font-medium leading-[1.2] tracking-[-0.28px] text-body outline-none transition-colors data-[placeholder]:text-body-muted data-[state=open]:border-primary-500 focus-visible:border-focus focus-visible:ring-2 focus-visible:ring-focus/20"
              >
                <SelectValue
                  class="min-w-0 flex-1"
                  placeholder="성별을 선택해 주세요"
                />
                <ChevronDown
                  class="size-xl shrink-0 text-body-muted transition-transform group-data-[state=open]:rotate-180"
                  aria-hidden="true"
                />
              </SelectTrigger>

              <SelectPortal>
                <SelectContent
                  class="z-50 w-[var(--reka-select-trigger-width)] overflow-hidden rounded-large border border-border bg-surface-card p-sm shadow-modal"
                  position="popper"
                  :side-offset="8"
                  align="start"
                >
                  <SelectViewport class="flex flex-col gap-xs">
                    <SelectItem
                      class="group/item relative flex min-h-touch-target cursor-pointer select-none items-center rounded-medium px-md py-sm outline-none data-[highlighted]:bg-primary-900 data-[state=checked]:text-primary-500"
                      value="남"
                    >
                      <SelectItemText class="flex-1 text-[14px] font-medium"
                        >남성</SelectItemText
                      >
                      <SelectItemIndicator class="text-primary-500">
                        <Check
                          class="size-xl"
                          :stroke-width="2.5"
                          aria-hidden="true"
                        />
                      </SelectItemIndicator>
                    </SelectItem>
                    <SelectItem
                      class="group/item relative flex min-h-touch-target cursor-pointer select-none items-center rounded-medium px-md py-sm outline-none data-[highlighted]:bg-primary-900 data-[state=checked]:text-primary-500"
                      value="여"
                    >
                      <SelectItemText class="flex-1 text-[14px] font-medium"
                        >여성</SelectItemText
                      >
                      <SelectItemIndicator class="text-primary-500">
                        <Check
                          class="size-xl"
                          :stroke-width="2.5"
                          aria-hidden="true"
                        />
                      </SelectItemIndicator>
                    </SelectItem>
                  </SelectViewport>
                </SelectContent>
              </SelectPortal>
            </SelectRoot>
            <p v-if="errors.gender" class="type-caption text-error">
              {{ errors.gender }}
            </p>
          </section>

          <section class="flex flex-col gap-sm">
            <label class="type-h4 text-body" for="sign-up-phone"
              >휴대폰 번호</label
            >
            <div class="flex gap-sm">
              <input
                id="sign-up-phone"
                :value="phoneNumber"
                autocomplete="tel"
                class="h-[52px] min-w-0 flex-1 rounded-medium border border-border-strong bg-surface-card px-md text-[16px] font-normal leading-[1.2] tracking-[-0.32px] text-body outline-none placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20"
                inputmode="tel"
                maxlength="13"
                pattern="01[016789]-?\d{3,4}-?\d{4}"
                placeholder="010-0000-0000"
                type="tel"
                :aria-invalid="errors.phoneNumber ? 'true' : undefined"
                :aria-describedby="
                  errors.phoneNumber ? 'sign-up-phone-error' : undefined
                "
                @input="updatePhoneNumberFromEvent($event)"
              />
              <Button
                :disabled="
                  verificationToken !== null ||
                  sendPhoneCodeMutation.isPending.value
                "
                :label="
                  verificationToken
                    ? '인증 완료'
                    : sendPhoneCodeMutation.isPending.value
                      ? '발송 중'
                      : phoneCodeRequested
                        ? '재요청'
                        : '인증하기'
                "
                variant="secondary"
                type="button"
                @click="requestPhoneCode"
              />
            </div>
            <p
              v-if="errors.phoneNumber"
              id="sign-up-phone-error"
              class="type-caption text-error"
            >
              {{ errors.phoneNumber }}
            </p>
            <div
              v-if="phoneCodeRequested || verificationToken"
              class="flex flex-col gap-sm"
            >
              <div class="flex gap-sm">
                <input
                  :value="phoneVerificationCode"
                  aria-label="휴대폰 인증번호"
                  class="h-[52px] min-w-0 flex-1 rounded-medium border border-border-strong bg-surface-card px-md text-[16px] font-normal leading-[1.2] tracking-[-0.32px] text-body outline-none placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20"
                  inputmode="numeric"
                  maxlength="6"
                  pattern="\d{6}"
                  placeholder="인증번호 6자리"
                  :disabled="verificationToken !== null"
                  type="text"
                  @input="
                    updatePhoneVerificationCode(inputValueFromEvent($event))
                  "
                />
                <Button
                  v-if="verificationToken === null"
                  :disabled="verifyPhoneCodeMutation.isPending.value"
                  :label="
                    verifyPhoneCodeMutation.isPending.value ? '확인 중' : '확인'
                  "
                  variant="secondary"
                  type="button"
                  @click="confirmPhoneCode"
                />
                <span
                  v-else
                  aria-hidden="true"
                  class="h-[52px] w-[80px] shrink-0"
                />
              </div>
              <p
                v-if="verificationToken === null"
                class="type-caption flex items-center justify-between text-body-muted"
              >
                <span>인증번호 유효시간</span>
                <span class="font-semibold tabular-nums text-error">
                  {{ phoneCodeTimerLabel }}
                </span>
              </p>
              <p
                class="type-caption"
                :class="
                  phoneVerificationMessage === '인증 완료했어요.'
                    ? 'text-success'
                    : 'text-body-muted'
                "
              >
                {{ phoneVerificationMessage }}
              </p>
            </div>
          </section>

          <section class="flex flex-col gap-sm">
            <label class="type-h4 text-body" for="sign-up-password"
              >로그인 비밀번호</label
            >
            <div class="relative">
              <input
                id="sign-up-password"
                v-model="loginPassword"
                :type="isPasswordVisible ? 'text' : 'password'"
                autocomplete="new-password"
                class="h-[52px] w-full rounded-medium border border-border-strong bg-surface-card px-md pr-[52px] text-[16px] font-normal leading-[1.2] tracking-[-0.32px] text-body outline-none placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20"
                placeholder="비밀번호를 입력하세요"
                maxlength="64"
                minlength="8"
                :aria-invalid="errors.loginPassword ? 'true' : undefined"
                :aria-describedby="
                  errors.loginPassword ? 'sign-up-password-error' : undefined
                "
              />
              <button
                :aria-label="
                  isPasswordVisible ? '비밀번호 숨기기' : '비밀번호 보기'
                "
                class="absolute right-md top-1/2 -translate-y-1/2 text-body-muted focus-visible:outline-2 focus-visible:outline-focus"
                type="button"
                @click="isPasswordVisible = !isPasswordVisible"
              >
                <EyeOff v-if="isPasswordVisible" class="size-xl" />
                <Eye v-else class="size-xl" />
              </button>
            </div>
            <p
              id="sign-up-password-error"
              class="type-caption"
              :class="errors.loginPassword ? 'text-error' : 'text-body-muted'"
            >
              {{ errors.loginPassword ?? '숫자, 영문 포함 8자 이상' }}
            </p>
          </section>

          <section class="flex flex-col gap-sm">
            <label class="type-h4 text-body">결제 비밀번호 (6자리)</label>
            <div class="grid grid-cols-6 gap-sm">
              <input
                v-for="(_, index) in paymentPasswordInputIds"
                :id="paymentPasswordInputIds[index]"
                :key="paymentPasswordInputIds[index]"
                :value="paymentDigit(index)"
                :aria-label="`결제 비밀번호 ${index + 1}번째 자리`"
                class="type-h3 h-[52px] min-w-0 rounded-medium border border-border-strong bg-surface-card text-center text-body outline-none focus:border-focus focus:ring-2 focus:ring-focus/20"
                inputmode="numeric"
                maxlength="1"
                pattern="\d"
                type="password"
                @input="updatePaymentPasswordFromEvent(index, $event)"
                @keydown="handlePaymentPasswordKeydown(index, $event)"
              />
            </div>
            <p
              class="type-caption"
              :class="errors.paymentPassword ? 'text-error' : 'text-body-muted'"
            >
              {{
                errors.paymentPassword ??
                '결제 시 사용할 숫자 6자리를 입력해주세요.'
              }}
            </p>
          </section>
        </div>

        <section
          class="rounded-large border border-border bg-surface-card p-xl shadow-card"
        >
          <label
            class="flex cursor-pointer items-center gap-md border-b border-border pb-md"
          >
            <CheckboxRoot
              :model-value="allTermsAgreed"
              class="flex size-xl shrink-0 items-center justify-center rounded-small border border-border-strong bg-surface-card outline-none data-[state=checked]:border-primary-500 data-[state=checked]:bg-primary-500 focus-visible:ring-2 focus-visible:ring-focus"
              @update:model-value="toggleAllTerms"
            >
              <CheckboxIndicator class="text-on-action">
                <Check class="size-lg" :stroke-width="3" />
              </CheckboxIndicator>
            </CheckboxRoot>
            <span class="type-body-medium text-body"
              >약관에 모두 동의합니다</span
            >
          </label>

          <div class="mt-md flex flex-col gap-md">
            <div
              v-for="(term, termId) in signUpTerms"
              :key="termId"
              class="flex items-center justify-between gap-md"
            >
              <span class="flex items-center gap-md">
                <CheckboxRoot
                  :model-value="isTermAgreed(termId)"
                  class="flex size-lg shrink-0 items-center justify-center rounded-small border border-border-strong bg-surface-card outline-none data-[state=checked]:border-primary-500 data-[state=checked]:bg-primary-500 focus-visible:ring-2 focus-visible:ring-focus"
                  :aria-label="term.label"
                  @update:model-value="setTermAgreement(termId, $event)"
                >
                  <CheckboxIndicator class="text-on-action">
                    <Check class="size-md" :stroke-width="3" />
                  </CheckboxIndicator>
                </CheckboxRoot>
                <span class="type-body text-body-secondary">{{
                  term.label
                }}</span>
              </span>
              <RouterLink
                :aria-label="`${term.title} 상세 보기`"
                :to="`/auth/sign-up/terms/${termId}`"
                class="flex size-touch-target shrink-0 items-center justify-center text-body-muted outline-none focus-visible:ring-2 focus-visible:ring-focus"
                @click="flushSignUpDraftSave"
              >
                <ChevronRight aria-hidden="true" class="size-md" />
              </RouterLink>
            </div>
          </div>
        </section>
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
            !isReadyToSubmit ||
            createUserMutation.isPending.value ||
            loginMutation.isPending.value
          "
        />
      </div>
    </form>
  </main>
</template>
