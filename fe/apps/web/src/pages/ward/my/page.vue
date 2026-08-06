<script setup lang="ts">
import { FileText, Handshake, LogOut, ShieldUser, UserRound } from '@lucide/vue'
import { Button, Modal } from '@pay-with/ui'
import { useQueryClient } from '@tanstack/vue-query'
import type { Component } from 'vue'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { clearAuthenticationSession } from '@/api/auth-session'

const router = useRouter()
const queryClient = useQueryClient()
const isLogoutConfirmOpen = ref(false)

const menuItems: Array<{
  label: string
  icon: Component
  tone: 'primary' | 'danger'
  action: () => void
}> = [
  {
    label: '내 정보',
    icon: UserRound,
    tone: 'primary',
    action: () => router.push({ name: 'ward-my-profile' }),
  },
  {
    label: '보호자 관리',
    icon: ShieldUser,
    tone: 'primary',
    action: () => router.push({ name: 'ward-my-guardian' }),
  },
  {
    label: '이용 약관',
    icon: Handshake,
    tone: 'primary',
    action: () =>
      router.push({
        name: 'ward-my-terms',
        params: { termId: 'serviceTerms' },
      }),
  },
  {
    label: '개인정보처리방침',
    icon: FileText,
    tone: 'primary',
    action: () =>
      router.push({
        name: 'ward-my-terms',
        params: { termId: 'privacyTerms' },
      }),
  },
  {
    label: '로그아웃',
    icon: LogOut,
    tone: 'danger',
    action: () => {
      isLogoutConfirmOpen.value = true
    },
  },
]

async function logout() {
  isLogoutConfirmOpen.value = false
  clearAuthenticationSession()
  queryClient.clear()
  await router.replace({ name: 'auth-sign-in' })
}
</script>

<template>
  <section class="flex flex-1 flex-col" aria-label="마이페이지 메뉴">
    <div class="grid gap-md">
      <Button
        v-for="item in menuItems"
        :key="item.label"
        class="min-h-[80px] w-full !gap-md rounded-[16px] text-[24px] font-bold leading-[1.2]"
        :label="item.label"
        :variant="item.tone === 'danger' ? 'outline-danger' : 'outline-primary'"
        size="large"
        @click="item.action"
      >
        <template #leading>
          <component
            :is="item.icon"
            class="size-7"
            :stroke-width="2.2"
            aria-hidden="true"
          />
        </template>
      </Button>
    </div>

    <Modal
      :open="isLogoutConfirmOpen"
      title="로그아웃 할까요?"
      description="다시 이용하려면 로그인해야 합니다."
      size="large"
      icon-tone="error"
      @update:open="isLogoutConfirmOpen = $event"
    >
      <template #icon>
        <LogOut class="size-xxl" :stroke-width="2.25" aria-hidden="true" />
      </template>

      <template #actions="{ close }">
        <div class="flex flex-col gap-md">
          <Button
            class="w-full"
            label="로그아웃"
            variant="danger"
            size="large"
            pill
            @click="logout"
          />
          <Button
            class="w-full"
            label="계속 이용하기"
            variant="outline-primary"
            size="large"
            pill
            @click="close"
          />
        </div>
      </template>
    </Modal>
  </section>
</template>
