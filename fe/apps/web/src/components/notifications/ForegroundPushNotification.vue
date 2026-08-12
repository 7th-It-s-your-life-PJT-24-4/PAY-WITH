<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { useRouter } from 'vue-router'

import { usePushNotification } from '@/composables/usePushNotification'

const router = useRouter()
const { dismissForegroundNotification, foregroundNotification } =
  usePushNotification()

function openNotification() {
  const notification = foregroundNotification.value
  if (!notification) return

  dismissForegroundNotification()
  void router.push(notification.path)
}
</script>

<template>
  <section
    v-if="foregroundNotification"
    class="mb-lg rounded-large border border-primary-500/30 bg-primary-500/5 p-lg shadow-card"
    aria-label="새 알림"
  >
    <div class="flex items-start justify-between gap-md">
      <button
        class="min-w-0 flex-1 text-left"
        type="button"
        @click="openNotification"
      >
        <h2 class="font-semibold text-body">
          {{ foregroundNotification.title }}
        </h2>
        <p class="mt-xs text-sm leading-5 text-body-secondary">
          {{ foregroundNotification.body }}
        </p>
      </button>
      <Button
        class="shrink-0"
        label="닫기"
        variant="secondary"
        size="small"
        @click="dismissForegroundNotification"
      />
    </div>
  </section>
</template>
