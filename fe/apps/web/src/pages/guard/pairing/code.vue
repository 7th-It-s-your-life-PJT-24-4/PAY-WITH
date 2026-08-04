<script setup lang="ts">
import { Button, Toast } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { ChevronLeft, Copy, Link, Share2 } from '@lucide/vue'
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { guardPairingStatusOptions } from '@/lib/query/guard/home'
import { usePairingStore } from '@/stores/pairing.store'

const router = useRouter()
const pairingStore = usePairingStore()
const toastMessage = ref('')
const isToastOpen = ref(false)
const remainingSeconds = ref(0)
const isPairingCompleted = ref(false)
let countdownTimer: ReturnType<typeof globalThis.setInterval> | undefined

const shouldPollPairingStatus = computed(
  () =>
    Boolean(pairingStore.code) &&
    remainingSeconds.value > 0 &&
    !isPairingCompleted.value,
)
const pairingStatus = useQuery(
  guardPairingStatusOptions(shouldPollPairingStatus),
)
const hasConnectedWard = computed(
  () => (pairingStatus.data.value?.wards.length ?? 0) > 0,
)

const formattedRemainingTime = computed(() => {
  const minutes = Math.floor(remainingSeconds.value / 60)
  const seconds = String(remainingSeconds.value % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
})

function updateRemainingTime() {
  if (!pairingStore.expiresAt) {
    remainingSeconds.value = 0
    return
  }

  remainingSeconds.value = Math.max(
    0,
    Math.ceil((Date.parse(pairingStore.expiresAt) - Date.now()) / 1000),
  )
}

function startCountdown() {
  updateRemainingTime()
  if (countdownTimer) globalThis.clearInterval(countdownTimer)
  countdownTimer = globalThis.setInterval(updateRemainingTime, 1000)
}

watch(hasConnectedWard, (isConnected) => {
  if (!isConnected) return
  isPairingCompleted.value = true
  pairingStore.markPaired()
  if (countdownTimer) globalThis.clearInterval(countdownTimer)
})

async function issueCode() {
  if (await pairingStore.issueCode()) {
    startCountdown()
  }
}

function showToast(message: string) {
  toastMessage.value = message
  isToastOpen.value = false
  globalThis.requestAnimationFrame(() => {
    isToastOpen.value = true
  })
}

async function copyText(value: string, message: string) {
  try {
    if (!globalThis.navigator.clipboard)
      throw new Error('clipboard unavailable')
    await globalThis.navigator.clipboard.writeText(value)
  } catch {
    const textarea = globalThis.document.createElement('textarea')
    textarea.value = value
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    globalThis.document.body.append(textarea)
    textarea.select()
    globalThis.document.execCommand('copy')
    textarea.remove()
  }

  showToast(message)
}

function copyCode() {
  return copyText(pairingStore.code, '코드가 복사되었습니다')
}

function copyLink() {
  return copyText(pairingStore.inviteUrl, '링크가 복사되었습니다')
}

async function sharePairingLink() {
  const shareData = {
    title: 'PayWith 시니어 연결',
    text: `인증 코드 ${pairingStore.code}를 입력해 주세요.`,
    url: pairingStore.inviteUrl,
  }

  if (globalThis.navigator.share) {
    await globalThis.navigator.share(shareData)
    return
  }

  await copyLink()
}

onMounted(async () => {
  if (!pairingStore.code) {
    await issueCode()
    return
  }

  startCountdown()
})

onBeforeUnmount(() => {
  if (countdownTimer) globalThis.clearInterval(countdownTimer)
})
</script>

<template>
  <main class="mx-auto min-h-screen w-full max-w-[390px] bg-white">
    <header class="flex h-[44px] items-center justify-between">
      <button
        class="flex size-[44px] items-center justify-center text-[#3b3e43]"
        type="button"
        aria-label="뒤로 가기"
        @click="router.push({ name: 'guard-home' })"
      >
        <ChevronLeft class="size-6" :stroke-width="1.8" aria-hidden="true" />
      </button>
      <span class="size-[44px]" aria-hidden="true" />
    </header>

    <section class="px-mobile-gutter pt-[5px]" aria-labelledby="pairing-title">
      <h1
        id="pairing-title"
        class="text-[24px] font-bold leading-[1.2] tracking-[-0.48px] text-black"
      >
        시니어 연결하기
      </h1>
      <p
        class="mt-xs text-[16px] font-medium leading-[1.2] tracking-[-0.32px] text-gray-600"
      >
        시니어와 연결하여<br />
        안전하게 자산을 보호하세요
      </p>

      <section
        class="relative mt-xl h-[97px] rounded-large bg-[#f6f7f8] px-md py-md text-center"
        aria-label="인증 코드"
      >
        <p
          class="text-[14px] font-medium leading-[1.2] tracking-[-0.28px] text-gray-500"
        >
          인증 코드
        </p>
        <p
          class="mt-[9px] text-[28px] font-semibold leading-[1.2] tracking-[2.24px] text-black"
        >
          {{ pairingStore.code || '-----' }}
        </p>
        <p
          class="absolute right-md top-md text-[14px] font-medium leading-[14px] tracking-[-0.2px] text-error"
        >
          {{ formattedRemainingTime }}
        </p>
      </section>

      <p
        v-if="pairingStore.errorMessage"
        class="mt-sm text-center text-[14px] font-medium leading-[1.4] text-error"
        role="alert"
      >
        {{ pairingStore.errorMessage }}
      </p>

      <button
        v-if="remainingSeconds === 0 || pairingStore.errorMessage"
        class="mx-auto mt-md block text-[14px] font-semibold text-primary-500 disabled:text-gray-400"
        type="button"
        :disabled="pairingStore.isIssuingCode"
        @click="issueCode"
      >
        {{ pairingStore.isIssuingCode ? '발급 중...' : '인증 코드 다시 발급' }}
      </button>

      <div class="mt-xl grid grid-cols-3 gap-[12px] px-[29px] text-center">
        <button
          class="flex flex-col items-center"
          type="button"
          @click="copyCode"
        >
          <span
            class="flex size-[48px] items-center justify-center rounded-full bg-gray-400 text-white"
          >
            <Copy class="size-6" :stroke-width="1.8" aria-hidden="true" />
          </span>
          <span
            class="mt-xxs text-[14px] font-medium leading-[26px] tracking-[-0.2px] text-gray-400"
          >
            코드 복사
          </span>
        </button>

        <button
          class="flex flex-col items-center"
          type="button"
          @click="copyLink"
        >
          <span
            class="flex size-[48px] items-center justify-center rounded-full bg-gray-400 text-white"
          >
            <Link class="size-6" :stroke-width="1.8" aria-hidden="true" />
          </span>
          <span
            class="mt-xxs text-[14px] font-medium leading-[26px] tracking-[-0.2px] text-gray-400"
          >
            링크 복사
          </span>
        </button>

        <button
          class="flex flex-col items-center"
          type="button"
          @click="sharePairingLink"
        >
          <span
            class="flex size-[48px] items-center justify-center rounded-full bg-gray-400 text-white"
          >
            <Share2 class="size-6" :stroke-width="1.8" aria-hidden="true" />
          </span>
          <span
            class="mt-xxs text-[14px] font-medium leading-[26px] tracking-[-0.2px] text-gray-400"
          >
            공유하기
          </span>
        </button>
      </div>

      <section
        v-if="hasConnectedWard"
        class="mt-xl rounded-large bg-primary-900/10 px-lg py-md text-center"
        aria-live="polite"
      >
        <p class="text-[16px] font-semibold text-primary-500">
          시니어와 연결되었어요.
        </p>
        <Button
          class="mt-md w-full"
          label="홈으로 이동"
          @click="router.replace({ name: 'guard-home' })"
        />
      </section>
    </section>

    <Toast
      v-model:open="isToastOpen"
      :message="toastMessage"
      :duration="2200"
    />
  </main>
</template>
