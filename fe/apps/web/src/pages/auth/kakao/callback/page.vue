<script setup lang="ts">
import { AlertCircle } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { startKakaoLogin } from '@/composables/useKakaoLogin'
import { useSignUpStore } from '@/stores/sign-up.store'

const route = useRoute()
const router = useRouter()
const signUpStore = useSignUpStore()

const authorizationCode = computed(() =>
  typeof route.query.code === 'string' ? route.query.code : null,
)

function moveToKakaoSignUp() {
  if (!authorizationCode.value) return

  signUpStore.startKakaoSignUp()
  router.replace('/auth/sign-up/details')
}

onMounted(moveToKakaoSignUp)
</script>

<template>
  <main
    v-if="!authorizationCode"
    class="mx-auto flex min-h-screen w-full max-w-[390px] items-center bg-surface px-mobile-gutter"
  >
    <section class="w-full text-center">
      <AlertCircle class="mx-auto size-xxl text-error" />
      <h1 class="type-h3 mt-lg text-body">
        카카오 로그인을 완료하지 못했어요.
      </h1>
      <p class="type-body-regular mt-sm text-body-muted">
        카카오에서 인가 코드를 받지 못했습니다.
      </p>
      <Button
        class="mt-xl"
        label="카카오 로그인 다시 시도"
        @click="startKakaoLogin"
      />
    </section>
  </main>
</template>
