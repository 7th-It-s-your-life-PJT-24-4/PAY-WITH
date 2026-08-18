import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it } from 'vitest'

import { requireSignUpRole } from '@/pages/auth/sign-up/-utils/sign-up-route-guard'
import { useSignUpStore } from '@/stores/sign-up.store'

describe('회원가입 상세 라우트 가드', () => {
  beforeEach(() => {
    sessionStorage.clear()
    setActivePinia(createPinia())
  })

  it('가입 유형이 없으면 첫 단계로 이동시킨다', () => {
    expect(
      requireSignUpRole({} as never, {} as never, () => undefined),
    ).toEqual({ name: 'auth-sign-up' })
  })

  it('가입 유형을 선택한 경우 상세 단계 접근을 허용한다', () => {
    useSignUpStore().setRole('guardian')

    expect(requireSignUpRole({} as never, {} as never, () => undefined)).toBe(
      true,
    )
  })
})
