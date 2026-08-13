<script setup lang="ts">
import { BellRing, X } from '@lucide/vue'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { usePushNotification } from '@/composables/usePushNotification'

const router = useRouter()
const { dismissForegroundNotification, foregroundNotification } =
  usePushNotification()

const actionLabel = computed(() => {
  const type = foregroundNotification.value?.data.type
  if (type === 'APPROVAL_REQUEST') return '승인 요청 확인하기'
  if (type === 'ANOMALY') return '이상 거래 상세 보기'
  return '확인하기'
})

async function openNotification() {
  const notification = foregroundNotification.value
  if (!notification) return

  dismissForegroundNotification()
  await router.push(notification.path)
}
</script>

<template>
  <Teleport to="body">
    <Transition
      enter-active-class="transition duration-200 ease-out"
      enter-from-class="-translate-y-3 opacity-0"
      leave-active-class="transition duration-150 ease-in"
      leave-to-class="-translate-y-3 opacity-0"
    >
      <aside
        v-if="foregroundNotification"
        class="fixed inset-x-0 top-[calc(12px+env(safe-area-inset-top))] z-[100] mx-auto w-[calc(100%-32px)] max-w-[358px] rounded-2xl border border-primary-500/20 bg-white p-4 shadow-[0_12px_32px_rgb(15_23_42/18%)]"
        role="alert"
        aria-live="assertive"
        aria-labelledby="foreground-push-title"
      >
        <div class="flex items-start gap-3">
          <span
            class="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary-500/10 text-primary-500"
          >
            <BellRing class="size-5" aria-hidden="true" />
          </span>

          <div class="min-w-0 flex-1">
            <h2
              id="foreground-push-title"
              class="break-keep text-[16px] font-semibold leading-6 text-red-500"
            >
              {{ foregroundNotification.title }}
            </h2>
            <p class="mt-1 break-keep text-[14px] leading-5 text-gray-900">
              {{ foregroundNotification.body }}
            </p>
            <button
              class="mt-3 min-h-10 rounded-lg bg-primary-500 px-4 text-[14px] font-semibold text-white outline-none focus-visible:ring-2 focus-visible:ring-focus focus-visible:ring-offset-2"
              type="button"
              @click="openNotification"
            >
              {{ actionLabel }}
            </button>
          </div>

          <button
            class="flex size-9 shrink-0 items-center justify-center rounded-full text-gray-500 outline-none hover:bg-gray-100 focus-visible:ring-2 focus-visible:ring-focus"
            type="button"
            aria-label="알림 닫기"
            @click="dismissForegroundNotification"
          >
            <X class="size-5" aria-hidden="true" />
          </button>
        </div>
      </aside>
    </Transition>
  </Teleport>
</template>
