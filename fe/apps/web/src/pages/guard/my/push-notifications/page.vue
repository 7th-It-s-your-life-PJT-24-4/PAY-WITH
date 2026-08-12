<script setup lang="ts">
import { SwitchRoot, SwitchThumb } from 'reka-ui'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import switchThumbUrl from '@/assets/icons/switch-thumb.svg'
import { usePushNotification } from '@/composables/usePushNotification'
import GuardMyHeader from '@/pages/guard/my/-components/GuardMyHeader.vue'

const router = useRouter()
const isUpdating = ref(false)
const {
  disablePushNotifications,
  enablePushNotifications,
  errorMessage,
  isEnabled,
  isSyncing,
} = usePushNotification()

async function updatePushNotificationSetting(enabled: boolean) {
  if (isUpdating.value || enabled === isEnabled.value) return

  isUpdating.value = true
  try {
    if (enabled) await enablePushNotifications()
    else await disablePushNotifications()
  } finally {
    isUpdating.value = false
  }
}
</script>

<template>
  <main class="min-h-screen pb-[calc(66px+env(safe-area-inset-bottom))]">
    <GuardMyHeader
      title="푸시알림설정"
      show-back
      @back="router.push({ name: 'guard-my' })"
    />

    <section
      class="w-[375px] max-w-full py-xs"
      aria-labelledby="push-notification-label"
    >
      <div class="px-mobile-gutter">
        <div class="pl-xs">
          <div class="flex min-h-11 items-center justify-between">
            <div class="min-w-0 pr-md font-medium tracking-[-0.2px]">
              <h2
                id="push-notification-label"
                class="text-[16px] leading-6 text-[#232529]"
              >
                알림 받기
              </h2>
              <p
                id="push-notification-description"
                class="text-[12px] leading-[18px] text-[#b5b9c0]"
              >
                다양한 알림을 실시간으로 받아요.
              </p>
            </div>

            <SwitchRoot
              :model-value="isEnabled"
              class="group flex h-6 w-11 shrink-0 cursor-pointer items-center rounded-xl bg-[#b5b9c0] p-0.5 outline-none transition-colors data-[state=checked]:bg-[#4c74ff] focus-visible:ring-2 focus-visible:ring-focus focus-visible:ring-offset-2 disabled:cursor-wait"
              :disabled="isUpdating || isSyncing"
              aria-labelledby="push-notification-label"
              aria-describedby="push-notification-description"
              @update:model-value="updatePushNotificationSetting"
            >
              <SwitchThumb
                class="block size-5 shrink-0 transition-transform duration-200 data-[state=checked]:translate-x-5 motion-reduce:transition-none"
              >
                <img
                  class="block size-5"
                  :src="switchThumbUrl"
                  width="20"
                  height="20"
                  alt=""
                  aria-hidden="true"
                />
              </SwitchThumb>
            </SwitchRoot>
          </div>

          <p
            v-if="errorMessage"
            class="mt-sm break-keep text-[12px] font-medium leading-[18px] text-error"
            role="alert"
          >
            {{ errorMessage }}
          </p>
        </div>
      </div>
    </section>
  </main>
</template>
