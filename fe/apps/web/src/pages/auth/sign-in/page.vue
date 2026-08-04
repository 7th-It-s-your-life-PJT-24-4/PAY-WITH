<script setup lang="ts">
import { Eye, EyeOff, LockKeyhole, Phone } from '@lucide/vue'
import { computed, ref, useId } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getApiErrorMessage } from '@/api/error'
import { getUserIdFromAccessToken } from '@/api/token-storage'
import { getUser } from '@/api/users'
import { useLoginMutation } from '@/composables/useLoginMutation'

const phoneNumber = ref('')
const password = ref('')
const isPasswordVisible = ref(false)

const router = useRouter()
const route = useRoute()
const loginMutation = useLoginMutation()
const formError = ref(
  route.query.signup === 'completed'
    ? '가입이 완료됐어요. 전화번호와 비밀번호로 로그인해 주세요.'
    : '',
)
const phoneInputId = useId()
const passwordInputId = useId()
const passwordInputType = computed(() =>
  isPasswordVisible.value ? 'text' : 'password',
)

async function submitLogin() {
  formError.value = ''

  try {
    const token = await loginMutation.mutateAsync({
      phone: phoneNumber.value,
      password: password.value,
    })
    const userId = getUserIdFromAccessToken(token.accessToken)
    if (!userId) {
      throw new Error('로그인 사용자 정보를 확인할 수 없습니다.')
    }

    const user = await getUser(userId)
    await router.replace(user.role === 'GUARD' ? '/guard' : '/ward')
  } catch (error) {
    formError.value = await getApiErrorMessage(
      error,
      '전화번호 또는 비밀번호를 다시 확인해 주세요.',
    )
  }
}
</script>

<template>
  <main
    class="relative flex min-h-screen items-center justify-center overflow-hidden bg-white px-5 py-[123px]"
  >
    <div
      aria-hidden="true"
      class="pointer-events-none absolute inset-0 opacity-40"
    >
      <div
        class="absolute -left-24 -top-24 size-96 rounded-full bg-[#00b1d2]/20 blur-[50px]"
      />
      <div
        class="absolute -bottom-24 -right-24 size-96 rounded-full bg-[#9ce8ff]/20 blur-[50px]"
      />
    </div>

    <section
      class="relative flex w-full max-w-[390px] flex-col items-center rounded-xl bg-white p-6 shadow-[0_2px_4px_rgba(8,13,18,0.06)]"
    >
      <header class="pb-8">
        <div class="flex flex-col items-center gap-1">
          <div class="flex h-[42px] items-end justify-center pb-1.5">
            <svg
              aria-hidden="true"
              class="h-9 w-[38px]"
              fill="none"
              viewBox="0 0 38 36"
            >
              <path
                d="M6 9.5 19 2l13 7.5v15L19 32 6 24.5v-15Z"
                fill="#00B1D2"
              />
              <path
                d="m11 13 8 4.5 8-4.5M19 17.5V27"
                stroke="#F4F9FA"
                stroke-linecap="round"
                stroke-linejoin="round"
                stroke-width="2.4"
              />
            </svg>
          </div>
          <h1
            class="pt-2 text-center text-2xl font-bold leading-8 tracking-[-0.6px] text-[#00b1d2]"
          >
            PayWith
          </h1>
          <p
            class="text-center text-sm font-medium tracking-[-0.28px] text-[#5c6770]"
          >
            함께 나누는 안전한 결제
          </p>
        </div>
      </header>

      <form class="flex w-full flex-col gap-5" @submit.prevent="submitLogin">
        <div class="flex flex-col gap-1">
          <label
            :for="phoneInputId"
            class="px-1 text-base font-semibold leading-[1.2] tracking-[-0.32px] text-[#171c1e]"
          >
            전화번호
          </label>
          <div class="relative">
            <Phone
              aria-hidden="true"
              class="pointer-events-none absolute left-4 top-1/2 size-4 -translate-y-1/2 text-[#6d797e]"
              :stroke-width="1.8"
            />
            <input
              :id="phoneInputId"
              v-model="phoneNumber"
              autocomplete="tel"
              class="h-[52px] w-full rounded-lg border border-[#dfe6ec] bg-[#eff4f7] py-3.5 pl-12 pr-4 text-sm tracking-[-0.28px] text-[#171c1e] outline-none placeholder:text-[#6d797e] focus:border-[#00b1d2] focus:ring-2 focus:ring-[#00b1d2]/20"
              inputmode="tel"
              placeholder="전화번호를 입력하세요"
              type="tel"
            />
          </div>
        </div>

        <div class="flex flex-col gap-1">
          <label
            :for="passwordInputId"
            class="px-1 text-base font-semibold leading-[1.2] tracking-[-0.32px] text-[#171c1e]"
          >
            비밀번호
          </label>
          <div class="relative">
            <LockKeyhole
              aria-hidden="true"
              class="pointer-events-none absolute left-4 top-1/2 h-5 w-4 -translate-y-1/2 text-[#6d797e]"
              :stroke-width="1.8"
            />
            <input
              :id="passwordInputId"
              v-model="password"
              :type="passwordInputType"
              autocomplete="current-password"
              class="h-[52px] w-full rounded-lg border border-[#dfe6ec] bg-[#eff4f7] py-3.5 pl-12 pr-12 text-sm tracking-[-0.28px] text-[#171c1e] outline-none placeholder:text-[#6d797e] focus:border-[#00b1d2] focus:ring-2 focus:ring-[#00b1d2]/20"
              placeholder="비밀번호를 입력하세요"
            />
            <button
              :aria-label="
                isPasswordVisible ? '비밀번호 숨기기' : '비밀번호 보기'
              "
              :aria-pressed="isPasswordVisible"
              class="absolute right-4 top-1/2 flex size-6 -translate-y-1/2 items-center justify-center rounded text-[#6d797e] outline-none focus-visible:ring-2 focus-visible:ring-[#00b1d2]"
              type="button"
              @click="isPasswordVisible = !isPasswordVisible"
            >
              <EyeOff
                v-if="isPasswordVisible"
                aria-hidden="true"
                class="size-5"
                :stroke-width="1.8"
              />
              <Eye
                v-else
                aria-hidden="true"
                class="size-5"
                :stroke-width="1.8"
              />
            </button>
          </div>
        </div>

        <button
          class="h-14 w-full rounded-lg bg-[#00b1d2] text-base font-semibold tracking-[-0.32px] text-[#f4f9fa] shadow-[0_4px_6px_-1px_rgba(0,0,0,0.1),0_2px_4px_-2px_rgba(0,0,0,0.1)] transition-colors hover:bg-[#009ab7] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#00b1d2]"
          type="submit"
          :disabled="loginMutation.isPending.value"
        >
          {{ loginMutation.isPending.value ? '로그인 중...' : '로그인' }}
        </button>

        <p v-if="formError" class="text-center text-sm text-error" role="alert">
          {{ formError }}
        </p>

        <nav aria-label="계정 도움말" class="flex justify-center">
          <RouterLink
            class="text-sm font-medium leading-[22px] text-[#3d494d] hover:text-[#00b1d2]"
            to="/auth/sign-up"
            >회원가입</RouterLink
          >
        </nav>
      </form>
    </section>
  </main>
</template>
