<script setup lang="ts">
import { BellRing, X } from '@lucide/vue'
import { useRouter } from 'vue-router'

import { usePushNotification } from '@/composables/usePushNotification'

const router = useRouter()
const { dismissForegroundNotification, foregroundNotification } =
  usePushNotification()

async function openNotification(): Promise<void> {
  const notification = foregroundNotification.value
  if (!notification) return
  dismissForegroundNotification()
  await router.push(notification.path)
}
</script>

<template>
  <aside
    v-if="foregroundNotification"
    class="fixed inset-x-4 top-[calc(12px+env(safe-area-inset-top))] z-[80] mx-auto flex max-w-[420px] items-start gap-3 rounded-2xl border border-primary-500/20 bg-white p-4 shadow-xl"
    role="status"
    aria-live="polite"
  >
    <button
      class="flex min-w-0 flex-1 items-start gap-3 text-left outline-none focus-visible:ring-2 focus-visible:ring-primary-500"
      type="button"
      @click="openNotification"
    >
      <span
        class="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary-500/10 text-primary-500"
      >
        <BellRing class="size-5" :stroke-width="2" aria-hidden="true" />
      </span>
      <span class="min-w-0">
        <strong class="block text-sm font-semibold text-gray-900">
          {{ foregroundNotification.title }}
        </strong>
        <span class="mt-1 line-clamp-2 block text-sm text-gray-700">
          {{ foregroundNotification.body }}
        </span>
      </span>
    </button>
    <button
      class="flex size-8 shrink-0 items-center justify-center rounded-full text-gray-600 outline-none focus-visible:ring-2 focus-visible:ring-primary-500"
      type="button"
      aria-label="알림 닫기"
      @click="dismissForegroundNotification"
    >
      <X class="size-4" aria-hidden="true" />
    </button>
  </aside>
</template>
