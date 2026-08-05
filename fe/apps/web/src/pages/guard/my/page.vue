<script setup lang="ts">
import { ChevronRight } from '@lucide/vue'
import { ConfirmModal } from '@pay-with/ui'
import { useQueryClient } from '@tanstack/vue-query'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { clearAuthenticationSession } from '@/api/auth-session'
import { getApiErrorMessage } from '@/api/error'
import { getUserIdFromAccessToken, tokenStorage } from '@/api/token-storage'
import { useDeleteUserMutation } from '@/composables/useDeleteUserMutation'
import GuardMyHeader from '@/pages/guard/my/-components/GuardMyHeader.vue'

const router = useRouter()
const queryClient = useQueryClient()
const deleteUserMutation = useDeleteUserMutation()
const isLogoutConfirmOpen = ref(false)
const isDeleteConfirmOpen = ref(false)
const errorMessage = ref('')

const accessToken = tokenStorage.getAccessToken()
const currentUserId = accessToken ? getUserIdFromAccessToken(accessToken) : null

const menuItems = [
  { label: '내 정보', routeName: 'guard-my-profile', enabled: true },
  { label: '시니어 관리', routeName: 'guard-my-seniors', enabled: true },
  { label: '이용약관', routeName: '', enabled: false },
  { label: '개인정보처리방침', routeName: '', enabled: false },
] as const

function moveToMenu(routeName: string, enabled: boolean) {
  if (!enabled || !routeName) return
  void router.push({ name: routeName })
}

async function finishAuthenticationSession() {
  clearAuthenticationSession()
  queryClient.clear()
  await router.replace({ name: 'auth-sign-in' })
}

async function logout() {
  isLogoutConfirmOpen.value = false
  await finishAuthenticationSession()
}

async function withdraw() {
  if (!currentUserId) return

  errorMessage.value = ''
  try {
    await deleteUserMutation.mutateAsync(currentUserId)
    isDeleteConfirmOpen.value = false
    await finishAuthenticationSession()
  } catch (error) {
    isDeleteConfirmOpen.value = false
    errorMessage.value = await getApiErrorMessage(
      error,
      '탈퇴하지 못했어요. 잠시 후 다시 시도해 주세요.',
    )
  }
}
</script>

<template>
  <main class="min-h-screen pb-[calc(66px+env(safe-area-inset-bottom))]">
    <GuardMyHeader title="마이페이지" />

    <section aria-label="마이페이지 메뉴">
      <div class="px-mobile-gutter">
        <button
          v-for="item in menuItems"
          :key="item.label"
          class="flex h-11 w-full items-center justify-between py-[10px] text-left text-[16px] font-medium leading-6 tracking-[-0.2px] text-[#232529] outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-focus disabled:cursor-default"
          type="button"
          :disabled="!item.enabled"
          @click="moveToMenu(item.routeName, item.enabled)"
        >
          <span>{{ item.label }}</span>
          <span class="flex size-11 items-center justify-center">
            <ChevronRight
              class="size-5 text-[#3b3e43]"
              :stroke-width="1.6"
              aria-hidden="true"
            />
          </span>
        </button>
      </div>

      <div class="mt-md h-px bg-[#dfe6ec]" />

      <div class="mt-md px-mobile-gutter">
        <button
          class="flex h-11 w-full items-center py-[10px] text-[16px] font-medium leading-6 tracking-[-0.2px] text-error outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-focus"
          type="button"
          @click="isLogoutConfirmOpen = true"
        >
          로그아웃
        </button>
        <button
          class="flex h-11 w-full items-center py-[10px] text-[16px] font-medium leading-6 tracking-[-0.2px] text-gray-800 outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-focus"
          type="button"
          @click="isDeleteConfirmOpen = true"
        >
          탈퇴하기
        </button>
      </div>
    </section>

    <p
      v-if="errorMessage"
      class="mt-lg px-mobile-gutter text-[14px] font-medium text-error"
      role="alert"
    >
      {{ errorMessage }}
    </p>

    <ConfirmModal
      v-model:open="isLogoutConfirmOpen"
      title="로그아웃 할까요?"
      @confirm="logout"
    />
    <ConfirmModal
      v-model:open="isDeleteConfirmOpen"
      title="탈퇴할까요?"
      :confirm-disabled="deleteUserMutation.isPending.value"
      @confirm="withdraw"
    />
  </main>
</template>
