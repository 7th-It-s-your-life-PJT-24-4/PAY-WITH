import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { useSignUpStore } from '@/stores/sign-up.store'

const draft = {
  fullName: '홍길동',
  phoneNumber: '010-1234-5678',
  birthDate: '1990.01.02',
  gender: '남' as const,
  loginPassword: 'password123',
  paymentPassword: '123456',
  serviceTerms: true,
  privacyTerms: true,
  identifierTerms: true,
}

describe('sign-up store', () => {
  beforeEach(() => {
    sessionStorage.clear()
    setActivePinia(createPinia())
  })

  it('약관 화면 이동 중에는 민감 정보를 포함한 초안을 메모리에 유지한다', () => {
    const store = useSignUpStore()

    store.setDraft(draft)

    expect(store.draft).toEqual(draft)
  })

  it('새로고침 후에는 비민감 필드만 세션 저장소에서 복원한다', () => {
    const store = useSignUpStore()
    store.setDraft(draft)

    const stored = JSON.parse(
      sessionStorage.getItem('pay-with:sign-up-draft') ?? '{}',
    ) as Record<string, unknown>
    expect(stored).not.toHaveProperty('loginPassword')
    expect(stored).not.toHaveProperty('paymentPassword')

    setActivePinia(createPinia())

    expect(useSignUpStore().draft).toEqual({
      ...draft,
      loginPassword: '',
      paymentPassword: '',
    })
  })

  it('가입 완료 후 저장된 초안을 제거한다', () => {
    const store = useSignUpStore()
    store.setDraft(draft)

    store.clearDraft()

    expect(store.draft).toBeNull()
    expect(sessionStorage.getItem('pay-with:sign-up-draft')).toBeNull()
  })

  it('휴대폰 인증 토큰은 약관 이동용 메모리에만 유지한다', () => {
    const store = useSignUpStore()

    store.setPhoneVerification({
      phone: '01012345678',
      verificationToken: 'verification-token',
    })

    expect(store.phoneVerification).toMatchObject({
      phone: '01012345678',
    })
    expect(sessionStorage.getItem('pay-with:sign-up-draft')).toBeNull()

    store.clearPhoneVerification()
    expect(store.phoneVerification).toBeNull()
  })
})
