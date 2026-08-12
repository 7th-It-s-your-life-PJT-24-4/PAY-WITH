<script setup lang="ts">
import { BellRing, Settings } from '@lucide/vue'
import { Button } from '@pay-with/ui'

import { usePushNotification } from '@/composables/usePushNotification'

withDefaults(
  defineProps<{
    description?: string
  }>(),
  {
    description: '송금 결과와 보호자 처리 결과를 놓치지 않도록 알려드려요.',
  },
)

const {
  availability,
  errorMessage,
  isSyncing,
  permission,
  requestPermission,
  shouldShowPermissionCard,
} = usePushNotification()
</script>

<template>
  <section
    v-if="shouldShowPermissionCard"
    class="w-full rounded-large border border-primary-500/30 bg-surface-card p-xl shadow-card"
    aria-labelledby="ward-push-permission-title"
  >
    <div class="flex items-start gap-md">
      <span
        class="flex size-12 shrink-0 items-center justify-center rounded-full bg-primary-900 text-primary-300"
        aria-hidden="true"
      >
        <Settings
          v-if="permission === 'denied'"
          class="size-6"
          :stroke-width="2.25"
        />
        <BellRing v-else class="size-6" :stroke-width="2.25" />
      </span>

      <div class="min-w-0 flex-1">
        <h2
          id="ward-push-permission-title"
          class="type-h3 text-body break-keep leading-tight font-bold"
        >
          {{
            permission === 'denied'
              ? '브라우저에서 알림을 켜주세요'
              : availability === 'unsupported'
                ? '이 기기에서는 알림을 사용할 수 없어요'
                : '중요한 거래 알림을 받아보세요'
          }}
        </h2>

        <p
          class="type-body mt-xs text-body-secondary break-keep leading-relaxed"
        >
          <template v-if="permission === 'denied'">
            브라우저나 휴대폰 설정에서 PayWith 알림 권한을 직접 켜주세요.
          </template>
          <template v-else-if="availability === 'unsupported'">
            이 브라우저에서는 알림을 지원하지 않아요. 지원되는 브라우저나 앱
            설치 상태를 확인해 주세요.
          </template>
          <template v-else>
            {{ description }}
          </template>
        </p>

        <p
          v-if="errorMessage"
          class="type-body-medium mt-sm text-error"
          role="alert"
        >
          {{ errorMessage }}
        </p>

        <Button
          v-if="permission === 'default' && availability === 'available'"
          class="mt-lg w-full"
          :label="isSyncing ? '연결 중...' : '알림 켜기'"
          size="large"
          :disabled="isSyncing"
          @click="requestPermission"
        />
      </div>
    </div>
  </section>
</template>
