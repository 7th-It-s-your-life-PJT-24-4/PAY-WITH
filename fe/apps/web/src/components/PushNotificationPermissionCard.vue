<script setup lang="ts">
import { BellRing, Settings } from '@lucide/vue'

import { usePushNotification } from '@/composables/usePushNotification'

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
    class="mx-mobile-gutter mb-lg rounded-2xl border border-primary-500/20 bg-primary-500/5 p-4"
    aria-labelledby="push-permission-title"
  >
    <div class="flex items-start gap-3">
      <span
        class="flex size-10 shrink-0 items-center justify-center rounded-full bg-white text-primary-500 shadow-sm"
      >
        <Settings
          v-if="permission === 'denied'"
          class="size-5"
          aria-hidden="true"
        />
        <BellRing v-else class="size-5" aria-hidden="true" />
      </span>
      <div class="min-w-0 flex-1">
        <h2 id="push-permission-title" class="font-semibold text-gray-900">
          {{
            permission === 'denied'
              ? '브라우저에서 알림을 허용해 주세요'
              : availability === 'unsupported'
                ? '이 환경에서는 푸시 알림을 사용할 수 없어요'
                : '중요한 거래 알림을 받아보세요'
          }}
        </h2>
        <p class="mt-1 break-keep text-sm leading-5 text-gray-700">
          <template v-if="permission === 'denied'">
            브라우저 또는 기기 설정에서 PayWith 알림 권한을 직접 허용해 주세요.
          </template>
          <template v-else-if="availability === 'unsupported'">
            iPhone은 Safari에서 홈 화면에 추가한 뒤 알림을 사용할 수 있어요.
          </template>
          <template v-else>
            승인 요청과 이상 거래를 놓치지 않도록 알림을 보내드려요.
          </template>
        </p>
        <p v-if="errorMessage" class="mt-2 text-sm text-error" role="alert">
          {{ errorMessage }}
        </p>
        <button
          v-if="permission === 'default' && availability === 'available'"
          class="mt-3 rounded-lg bg-primary-500 px-4 py-2 text-sm font-semibold text-white disabled:opacity-50"
          type="button"
          :disabled="isSyncing"
          @click="requestPermission"
        >
          {{ isSyncing ? '연결 중...' : '알림 받기' }}
        </button>
      </div>
    </div>
  </section>
</template>
