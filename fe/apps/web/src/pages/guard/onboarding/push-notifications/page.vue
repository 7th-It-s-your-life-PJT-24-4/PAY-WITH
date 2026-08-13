<script setup lang="ts">
import { BellRing, ShieldCheck } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { usePushNotification } from '@/composables/usePushNotification'

const router = useRouter()
const {
  availability,
  enablePushNotifications,
  errorMessage,
  isEnabled,
  isSyncing,
  permission,
} = usePushNotification()

const canEnableNotifications = computed(
  () =>
    availability.value === 'available' &&
    permission.value !== 'denied' &&
    !isEnabled.value,
)

const primaryLabel = computed(() => {
  if (isSyncing.value) return '알림 연결 중...'
  if (canEnableNotifications.value) return '알림 받고 시작하기'
  return '홈으로'
})

const statusMessage = computed(() => {
  if (permission.value === 'denied') {
    return '알림이 차단되어 있어요. 나중에 마이페이지나 브라우저 설정에서 다시 허용할 수 있어요.'
  }
  if (availability.value === 'unsupported') {
    return '현재 브라우저에서는 푸시 알림을 사용할 수 없어요. 나중에 지원되는 환경에서 설정해 주세요.'
  }
  if (availability.value === 'disabled') {
    return '현재 환경에서는 푸시 알림이 비활성화되어 있어요. 나중에 마이페이지에서 다시 설정할 수 있어요.'
  }
  if (isEnabled.value) return '이상 거래 알림을 받을 준비가 완료됐어요.'
  return ''
})

async function goHome() {
  await router.replace({ name: 'guard-home' })
}

async function handlePrimaryAction() {
  if (!canEnableNotifications.value) {
    await goHome()
    return
  }

  const enabled = await enablePushNotifications()
  if (enabled) await goHome()
}
</script>

<template>
  <main
    class="flex min-h-screen flex-col bg-surface px-mobile-gutter pb-[calc(24px+env(safe-area-inset-bottom))] pt-[calc(64px+env(safe-area-inset-top))]"
  >
    <section class="flex flex-1 flex-col items-center text-center">
      <div
        class="relative flex size-[112px] items-center justify-center rounded-full bg-primary-500/10 text-primary-500"
        aria-hidden="true"
      >
        <BellRing class="size-12" :stroke-width="1.8" />
        <span
          class="absolute bottom-1 right-1 flex size-9 items-center justify-center rounded-full border-4 border-surface bg-primary-500 text-white"
        >
          <ShieldCheck class="size-5" :stroke-width="2.5" />
        </span>
      </div>

      <h1 class="type-h1 mt-xl break-keep text-body">
        이상 거래를 발견하면<br />바로 알려드릴게요
      </h1>
      <p class="type-body mt-sm break-keep leading-6 text-body-muted">
        피보호자의 거래에서 위험 신호가 감지되면<br />보호자님께 푸시 알림을
        보내드려요.
      </p>

      <section
        class="mt-xxl w-full rounded-large border border-border bg-surface-card p-lg text-left shadow-card"
        aria-label="푸시 알림 안내"
      >
        <div class="flex items-start gap-md">
          <span
            class="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary-500/10 text-primary-500"
            aria-hidden="true"
          >
            <ShieldCheck class="size-xl" />
          </span>
          <div>
            <h2 class="type-h4 text-body">빠른 이상 거래 확인</h2>
            <p class="type-body mt-xs break-keep text-body-muted">
              알림을 누르면 해당 피보호자의 이상 거래 상세 화면으로 바로
              이동해요.
            </p>
          </div>
        </div>
      </section>

      <p
        v-if="statusMessage"
        class="type-caption mt-lg break-keep text-body-muted"
        role="status"
      >
        {{ statusMessage }}
      </p>
      <p
        v-if="errorMessage"
        class="type-caption mt-sm break-keep text-error"
        role="alert"
      >
        {{ errorMessage }}
      </p>
    </section>

    <div class="mt-xl flex w-full flex-col gap-xs">
      <Button
        class="w-full"
        :disabled="isSyncing || availability === 'checking'"
        :label="primaryLabel"
        size="large"
        @click="handlePrimaryAction"
      />
      <Button
        v-if="canEnableNotifications"
        class="w-full"
        label="나중에 하기"
        variant="text"
        @click="goHome"
      />
    </div>
  </main>
</template>
