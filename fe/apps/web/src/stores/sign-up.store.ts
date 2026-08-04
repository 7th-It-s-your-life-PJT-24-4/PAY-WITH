import { ref } from 'vue'
import { defineStore } from 'pinia'

import type {
  SignUpDetails,
  SignUpDetailsForm,
  SignUpRole,
} from '@/schemas/sign-up.schema'

type SignUpDraft = Omit<SignUpDetailsForm, 'gender'> & {
  gender?: SignUpDetailsForm['gender']
}
type PersistedSignUpDraft = Omit<
  Pick<
    SignUpDraft,
    | 'fullName'
    | 'phoneNumber'
    | 'birthDate'
    | 'gender'
    | 'avatarId'
    | 'serviceTerms'
    | 'privacyTerms'
    | 'identifierTerms'
  >,
  'gender'
> & {
  gender?: SignUpDetailsForm['gender']
}
type PhoneVerification = {
  phone: string
  verificationToken: string
}

const SIGN_UP_DRAFT_STORAGE_KEY = 'pay-with:sign-up-draft'
const SIGN_UP_ROLE_STORAGE_KEY = 'pay-with:sign-up-role'

function readStorage(key: string) {
  try {
    return globalThis.sessionStorage.getItem(key)
  } catch {
    return null
  }
}

function writeStorage(key: string, value: string) {
  try {
    globalThis.sessionStorage.setItem(key, value)
  } catch {
    // 저장소를 사용할 수 없어도 현재 탭의 Pinia 상태는 유지한다.
  }
}

function removeStorage(key: string) {
  try {
    globalThis.sessionStorage.removeItem(key)
  } catch {
    // 저장소 접근 실패는 가입 흐름을 막지 않는다.
  }
}

function readPersistedRole(): SignUpRole | null {
  const role = readStorage(SIGN_UP_ROLE_STORAGE_KEY)

  return role === 'senior' || role === 'guardian' ? role : null
}

function readPersistedDraft(): SignUpDraft | null {
  const raw = readStorage(SIGN_UP_DRAFT_STORAGE_KEY)
  if (!raw) return null

  try {
    const value: unknown = JSON.parse(raw)
    if (typeof value !== 'object' || value === null) return null

    const draft = value as Partial<PersistedSignUpDraft>
    const hasValidGender = draft.gender === '남' || draft.gender === '여'

    if (
      typeof draft.fullName !== 'string' ||
      typeof draft.phoneNumber !== 'string' ||
      typeof draft.birthDate !== 'string' ||
      (draft.avatarId !== null &&
        draft.avatarId !== undefined &&
        (!Number.isInteger(draft.avatarId) ||
          draft.avatarId < 1 ||
          draft.avatarId > 6)) ||
      typeof draft.serviceTerms !== 'boolean' ||
      typeof draft.privacyTerms !== 'boolean' ||
      typeof draft.identifierTerms !== 'boolean'
    ) {
      return null
    }

    return {
      fullName: draft.fullName,
      phoneNumber: draft.phoneNumber,
      birthDate: draft.birthDate,
      gender: hasValidGender ? draft.gender : undefined,
      avatarId: draft.avatarId ?? null,
      loginPassword: '',
      paymentPassword: '',
      serviceTerms: draft.serviceTerms,
      privacyTerms: draft.privacyTerms,
      identifierTerms: draft.identifierTerms,
    }
  } catch {
    return null
  }
}

function toPersistedDraft(draft: SignUpDraft): PersistedSignUpDraft {
  return {
    fullName: draft.fullName,
    phoneNumber: draft.phoneNumber,
    birthDate: draft.birthDate,
    gender: draft.gender,
    avatarId: draft.avatarId,
    serviceTerms: draft.serviceTerms,
    privacyTerms: draft.privacyTerms,
    identifierTerms: draft.identifierTerms,
  }
}

export const useSignUpStore = defineStore('sign-up', () => {
  const role = ref<SignUpRole | null>(readPersistedRole())
  const details = ref<SignUpDetails | null>(null)
  const draft = ref<SignUpDraft | null>(readPersistedDraft())
  const phoneVerification = ref<PhoneVerification | null>(null)

  function setRole(value: SignUpRole) {
    role.value = value
    writeStorage(SIGN_UP_ROLE_STORAGE_KEY, value)
  }

  function setDetails(value: SignUpDetails) {
    details.value = value
  }

  function setDraft(value: SignUpDraft) {
    draft.value = value
    writeStorage(
      SIGN_UP_DRAFT_STORAGE_KEY,
      JSON.stringify(toPersistedDraft(value)),
    )
  }

  function clearDraft() {
    draft.value = null
    removeStorage(SIGN_UP_DRAFT_STORAGE_KEY)
  }

  function setPhoneVerification(value: PhoneVerification) {
    phoneVerification.value = value
  }

  function clearPhoneVerification() {
    phoneVerification.value = null
  }

  return {
    role,
    details,
    draft,
    phoneVerification,
    setRole,
    setDetails,
    setDraft,
    clearDraft,
    setPhoneVerification,
    clearPhoneVerification,
  }
})
