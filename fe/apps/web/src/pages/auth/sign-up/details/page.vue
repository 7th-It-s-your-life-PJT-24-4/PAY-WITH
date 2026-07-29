<script setup lang="ts">
import { Camera, Check, ChevronRight, Eye, EyeOff, Plus } from '@lucide/vue'
import { AppHeader, Button, Input } from '@pay-with/ui'
import { CheckboxIndicator, CheckboxRoot } from 'reka-ui'
import { computed, ref, useId } from 'vue'
import { useRouter } from 'vue-router'
import { useForm } from 'vee-validate'

import {
  kakaoSignUpDetailsSchema,
  signUpDetailsSchema,
  type SignUpDetailsForm,
} from '@/schemas/sign-up.schema'
import { signUpTerms, type SignUpTermId } from '@/constants/sign-up-terms'
import { useSignUpStore } from '@/stores/sign-up.store'

const router = useRouter()
const signUpStore = useSignUpStore()
const isKakaoSignUp = computed(() => signUpStore.signUpMethod === 'kakao')
const profileImageUrl = ref<string | null>(
  signUpStore.kakaoProfile?.profileImageUrl ?? null,
)
const profileImageInput = ref<{ click: () => void } | null>(null)
const isPasswordVisible = ref(false)
const formError = ref('')
const paymentPasswordInputIds = Array.from({ length: 6 }, () => useId())

const { defineField, setErrors } = useForm<SignUpDetailsForm>({
  initialValues: signUpStore.details ?? {
    fullName: signUpStore.kakaoProfile?.name ?? '',
    phoneNumber: '',
    loginPassword: '',
    paymentPassword: '',
    serviceTerms: false,
    privacyTerms: false,
    identifierTerms: false,
  },
})

const [fullName] = defineField('fullName')
const [phoneNumber] = defineField('phoneNumber')
const [loginPassword] = defineField('loginPassword')
const [paymentPassword] = defineField('paymentPassword')
const [serviceTerms] = defineField('serviceTerms')
const [privacyTerms] = defineField('privacyTerms')
const [identifierTerms] = defineField('identifierTerms')

const requiredTermsAgreed = computed(
  () => serviceTerms.value && privacyTerms.value && identifierTerms.value,
)
const allTermsAgreed = computed(() => requiredTermsAgreed.value)
const activeDetailsSchema = computed(() =>
  isKakaoSignUp.value ? kakaoSignUpDetailsSchema : signUpDetailsSchema,
)
const isReadyToSubmit = computed(() => {
  const hasValidPhoneNumber = /^01[016789]-?\d{3,4}-?\d{4}$/.test(
    phoneNumber.value,
  )
  const hasValidLoginPassword =
    loginPassword.value.length >= 8 &&
    /[A-Za-z]/.test(loginPassword.value) &&
    /\d/.test(loginPassword.value)

  return (
    fullName.value.trim().length > 0 &&
    (isKakaoSignUp.value || hasValidPhoneNumber) &&
    hasValidLoginPassword &&
    /^\d{6}$/.test(paymentPassword.value) &&
    requiredTermsAgreed.value
  )
})

function formatPhoneNumber(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 11)

  if (digits.length <= 3) return digits
  if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`

  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
}

function updatePhoneNumber(value: string) {
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

function selectProfileImage() {
  profileImageInput.value?.click()
}

function updateProfileImage(event: unknown) {
  if (typeof event !== 'object' || event === null) return

  const target = Reflect.get(event, 'target')
  if (typeof target !== 'object' || target === null) return

  const files = Reflect.get(target, 'files')
  if (typeof files !== 'object' || files === null) return

  const image = Reflect.get(files, '0')

  if (!(image instanceof globalThis.Blob)) return

  profileImageUrl.value = globalThis.URL.createObjectURL(image)
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

function paymentDigit(index: number) {
  return paymentPassword.value[index] ?? ''
}

function submitSignUp() {
  const result = activeDetailsSchema.value.safeParse({
    fullName: fullName.value,
    phoneNumber: phoneNumber.value,
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

  signUpStore.setDetails(result.data)
  formError.value = ''
  router.push({
    name:
      signUpStore.role === 'guardian' ? 'guardian-pairing-code' : 'ward-home',
  })
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

        <div class="flex flex-col gap-xl">
          <Input
            v-model="fullName"
            label="성함 (실명)"
            placeholder="성함을 입력하세요"
          />

          <section v-if="!isKakaoSignUp" class="flex flex-col gap-sm">
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
                placeholder="010-0000-0000"
                type="tel"
                @input="updatePhoneNumberFromEvent($event)"
              />
              <Button label="인증하기" variant="secondary" type="button" />
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
            <p class="type-caption text-body-muted">숫자, 영문 포함 8자 이상</p>
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
                type="password"
                @input="updatePaymentPasswordFromEvent(index, $event)"
              />
            </div>
            <p class="type-caption text-body-muted">
              결제 시 사용할 숫자 6자리를 입력해주세요.
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
          label="가입하기"
          type="submit"
          :disabled="!isReadyToSubmit"
        />
      </div>
    </form>
  </main>
</template>
