<script setup lang="ts">
import { Check, Copy, Link, MessageCircle } from '@lucide/vue'
import { AppHeader, Button, Modal } from '@pay-with/ui'
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { usePairingStore } from '@/stores/pairing.store'

const router = useRouter()
const pairingStore = usePairingStore()
const isConfirmOpen = ref(pairingStore.status !== 'CODE_ISSUED')
const toastMessage = ref('')
const remainingSeconds = ref(0)
let countdownTimer: ReturnType<typeof globalThis.setInterval> | undefined
let toastTimer: ReturnType<typeof globalThis.setTimeout> | undefined

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

async function generateCode() {
  await pairingStore.issueCode()
  isConfirmOpen.value = false
  startCountdown()
}

function showToast(message: string) {
  toastMessage.value = message
  if (toastTimer) globalThis.clearTimeout(toastTimer)
  toastTimer = globalThis.setTimeout(() => {
    toastMessage.value = ''
  }, 2200)
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

async function shareWithKakao() {
  const shareData = {
    title: 'PayWith 시니어 연결',
    text: `인증 코드 ${pairingStore.code}를 입력해 주세요.`,
  }

  if (globalThis.navigator.share) {
    await globalThis.navigator.share(shareData)
    return
  }

  await copyLink()
}

onMounted(() => {
  if (pairingStore.status === 'CODE_ISSUED' && pairingStore.expiresAt) {
    startCountdown()
  }
})

onBeforeUnmount(() => {
  if (countdownTimer) globalThis.clearInterval(countdownTimer)
  if (toastTimer) globalThis.clearTimeout(toastTimer)
})
</script>

<template>
  <main
    class="mx-auto flex min-h-screen w-full max-w-[390px] flex-col bg-surface"
  >
    <AppHeader title="" show-back @back="router.back()" />

    <section class="flex-1 px-mobile-gutter pb-xl pt-md">
      <h1 class="type-h1 text-body">시니어 연결하기</h1>
      <p class="type-body mt-xs text-body-muted">
        시니어와 연결하여<br />안전하게 자산을 보호하세요
      </p>

      <div
        v-if="pairingStore.status === 'CODE_ISSUED'"
        class="mt-xl rounded-large bg-disabled/40 px-lg py-lg text-center"
      >
        <div class="flex items-start justify-center">
          <div class="flex-1 pl-[42px]">
            <p class="type-caption text-body-muted">인증 코드</p>
            <p
              class="type-numeric-display type-h1 mt-xs tracking-[0.08em] text-body"
            >
              {{ pairingStore.code }}
            </p>
          </div>
          <span class="type-caption mt-xs w-[42px] text-error">
            {{ formattedRemainingTime }}
          </span>
        </div>
      </div>

      <Button
        v-else
        class="mt-xl w-full"
        label="인증 코드 생성하기"
        @click="isConfirmOpen = true"
      />

      <div
        v-if="pairingStore.status === 'CODE_ISSUED'"
        class="mt-xl grid grid-cols-3 gap-lg text-center"
      >
        <button
          class="group flex flex-col items-center gap-xs"
          type="button"
          @click="copyCode"
        >
          <span
            class="flex size-[48px] items-center justify-center rounded-full bg-body-muted text-on-action"
          >
            <Copy class="size-xl" aria-hidden="true" />
          </span>
          <span class="type-body text-body-secondary">코드 복사</span>
        </button>
        <button
          class="group flex flex-col items-center gap-xs"
          type="button"
          @click="copyLink"
        >
          <span
            class="flex size-[48px] items-center justify-center rounded-full bg-body-muted text-on-action"
          >
            <Link class="size-xl" aria-hidden="true" />
          </span>
          <span class="type-body text-body-secondary">링크 복사</span>
        </button>
        <button
          class="group flex flex-col items-center gap-xs"
          type="button"
          @click="shareWithKakao"
        >
          <span
            class="flex size-[48px] items-center justify-center rounded-full bg-[#FEE500] text-[#191919]"
          >
            <MessageCircle
              class="size-xl"
              fill="currentColor"
              aria-hidden="true"
            />
          </span>
          <span class="type-body text-body-secondary">카카오톡 공유</span>
        </button>
      </div>
    </section>

    <Transition
      enter-active-class="transition"
      enter-from-class="translate-y-2 opacity-0"
      leave-active-class="transition"
      leave-to-class="translate-y-2 opacity-0"
    >
      <div
        v-if="toastMessage"
        class="type-body-medium fixed bottom-[64px] left-1/2 z-[60] -translate-x-1/2 rounded-medium bg-gray-900 px-lg py-md text-white shadow-modal"
        role="status"
      >
        {{ toastMessage }}
      </div>
    </Transition>

    <Modal
      :open="isConfirmOpen"
      title="인증 코드를 생성할까요?"
      :close-on-outside="false"
      @update:open="isConfirmOpen = $event"
    >
      <template #actions>
        <div class="grid grid-cols-2 gap-sm">
          <Button
            class="w-full"
            label="취소"
            variant="secondary"
            @click="isConfirmOpen = false"
          />
          <Button
            class="w-full"
            :label="pairingStore.isIssuingCode ? '생성 중' : '완료'"
            :disabled="pairingStore.isIssuingCode"
            @click="generateCode"
          >
            <template #leading><Check /></template>
          </Button>
        </div>
      </template>
    </Modal>
  </main>
</template>
