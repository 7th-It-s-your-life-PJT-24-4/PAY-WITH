<script setup lang="ts">
import { ChevronLeft } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { useRouter } from 'vue-router'

import { useSafeAccountStore } from '@/stores/safe-account.store'

const router = useRouter()
const safeAccountStore = useSafeAccountStore()

function completeSafeAccount() {
  safeAccountStore.reset()
  router.replace({ name: 'guard-home' })
}
</script>

<template>
  <main
    class="flex min-h-screen flex-col bg-white pb-[calc(96px+env(safe-area-inset-bottom))]"
  >
    <header class="grid h-11 grid-cols-[44px_1fr_44px] items-center">
      <button
        class="flex size-11 items-center justify-center text-[#3b3e43]"
        type="button"
        aria-label="뒤로 가기"
        @click="router.back()"
      >
        <ChevronLeft class="size-6" aria-hidden="true" />
      </button>
      <h1
        class="text-center text-[20px] font-bold leading-[1.2] tracking-[-0.4px] text-black"
      >
        안전계좌 추가
      </h1>
      <span aria-hidden="true" />
    </header>

    <section
      class="flex flex-1 items-start justify-center px-mobile-gutter pt-[120px] text-center"
      aria-labelledby="safe-account-confirm-title"
    >
      <h2
        id="safe-account-confirm-title"
        class="text-[32px] font-bold leading-[1.4] tracking-[-0.64px] text-black"
      >
        <span class="text-primary-500">{{
          safeAccountStore.recipientName
        }}</span
        >님을<br />
        안전계좌에<br />
        추가할까요?
      </h2>
    </section>

    <div
      class="fixed inset-x-0 bottom-0 z-30 mx-auto w-full max-w-[390px] bg-white px-mobile-gutter pb-[calc(20px+env(safe-area-inset-bottom))] pt-sm"
    >
      <Button
        class="w-full"
        label="안전계좌 추가하기"
        variant="guard-cta"
        size="guard-cta"
        @click="completeSafeAccount"
      />
    </div>
  </main>
</template>
