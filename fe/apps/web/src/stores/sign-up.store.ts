import { ref } from 'vue'
import { defineStore } from 'pinia'

import type { SignUpDetails, SignUpRole } from '@/schemas/sign-up.schema'

type KakaoProfile = {
  name: string | null
  profileImageUrl: string | null
}

export const useSignUpStore = defineStore('sign-up', () => {
  const role = ref<SignUpRole | null>(null)
  const details = ref<SignUpDetails | null>(null)
  const signUpMethod = ref<'standard' | 'kakao'>('standard')
  const kakaoProfile = ref<KakaoProfile | null>(null)

  function setRole(value: SignUpRole) {
    role.value = value
  }

  function setDetails(value: SignUpDetails) {
    details.value = value
  }

  function startKakaoSignUp(profile: KakaoProfile | null = null) {
    signUpMethod.value = 'kakao'
    kakaoProfile.value = profile
  }

  return {
    role,
    details,
    signUpMethod,
    kakaoProfile,
    setRole,
    setDetails,
    startKakaoSignUp,
  }
})
