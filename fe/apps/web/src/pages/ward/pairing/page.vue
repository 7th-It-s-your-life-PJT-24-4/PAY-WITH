<script setup lang="ts">
import { ShieldCheck, Smartphone } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed, onBeforeUnmount, ref } from 'vue'
import { useRouter } from 'vue-router'

import { useEnsureFocusedInputVisible } from '@/composables/useEnsureFocusedInputVisible'
import { getApiErrorMessage } from '@/api/error'
import { usePairingStore } from '@/stores/pairing.store'

const router = useRouter()
const pairingStore = usePairingStore()
const enteredCode = ref('')
const errorMessage = ref('')
const inputRef = ref<HTMLInputElement | null>(null)
let statusTimer: ReturnType<typeof setInterval> | undefined

useEnsureFocusedInputVisible(inputRef)

const codeDigits = computed(() =>
  Array.from({ length: 5 }, (_, index) => enteredCode.value[index] ?? ''),
)

function focusInput() {
  inputRef.value?.focus()
}

function handleInput(event: Event) {
  const target = event.target as HTMLInputElement
  const digits = target.value.replace(/\D/g, '')
  enteredCode.value = digits.slice(0, 5)
  target.value = enteredCode.value
  errorMessage.value = ''
}

async function connectGuardian() {
  if (!(await pairingStore.verifyCode(enteredCode.value))) {
    errorMessage.value =
      pairingStore.errorMessage ||
      (pairingStore.errorCode === 'PAIRING_004'
        ? '입력 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요.'
        : '인증 코드가 유효하지 않거나 만료되었습니다.')
    return
  }

  startStatusPolling()
}

function stopStatusPolling() {
  if (!statusTimer) return
  clearInterval(statusTimer)
  statusTimer = undefined
}

async function pollPairingStatus() {
  try {
    const status = await pairingStore.checkPairingRequestStatus()
    if (status === 'CONFIRMED') {
      stopStatusPolling()
      await router.replace({ name: 'ward-pairing-complete' })
    } else if (status === 'EXPIRED') {
      stopStatusPolling()
      errorMessage.value =
        '연결 시간이 만료되었습니다. 코드를 다시 확인해 주세요.'
    }
  } catch (error) {
    errorMessage.value = await getApiErrorMessage(
      error,
      '연결 상태를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.',
    )
  }
}

function startStatusPolling() {
  stopStatusPolling()
  void pollPairingStatus()
  statusTimer = setInterval(() => void pollPairingStatus(), 2_000)
}

onBeforeUnmount(stopStatusPolling)
</script>

<template>
  <div
    v-if="pairingStore.pairingRequestStatus === 'PENDING'"
    class="flex flex-col items-center text-center"
  >
    <span
      class="type-numeric-input flex size-16 items-center justify-center rounded-full bg-primary-500/10 text-primary-500"
      aria-hidden="true"
      >…</span
    >
    <h1 class="type-h1 mt-lg text-body">보호자 확인을 기다리고 있어요</h1>
    <p class="type-body mt-md text-body-secondary">
      보호자가 연결 요청을 확인하면<br />자동으로 연결됩니다.
    </p>
    <Button class="mt-xl w-full" label="연결 상태 확인 중" disabled />
  </div>

  <div v-else class="flex flex-col items-center text-center">
    <span
      class="flex size-[64px] items-center justify-center rounded-full bg-primary-500/10 text-primary-500"
      aria-hidden="true"
    >
      <ShieldCheck class="size-8" :stroke-width="2" />
    </span>

    <h1 class="type-h1 mt-lg text-body">
      보호자가 보낸 5자리 코드를 입력해주세요
    </h1>

    <div
      class="relative mt-xl flex w-full justify-center gap-sm cursor-pointer"
      @click="focusInput"
    >
      <input
        ref="inputRef"
        type="text"
        inputmode="numeric"
        class="absolute inset-0 size-full opacity-0 cursor-pointer"
        :value="enteredCode"
        aria-label="인증 코드 5자리 입력"
        @input="handleInput"
      />
      <span
        v-for="(digit, index) in codeDigits"
        :key="index"
        class="type-numeric-input flex h-[64px] w-[56px] items-center justify-center rounded-medium border-2 bg-surface-card text-body"
        :class="
          index === enteredCode.length && enteredCode.length < 5
            ? 'border-primary-500'
            : 'border-border'
        "
      >
        {{ digit }}
      </span>
    </div>

    <p v-if="errorMessage" class="type-body mt-sm text-error" role="alert">
      {{ errorMessage }}
    </p>

    <Button
      class="mt-xl w-full"
      :label="pairingStore.isVerifyingCode ? '확인 중' : '연결하기'"
      :disabled="enteredCode.length !== 5 || pairingStore.isVerifyingCode"
      @click="connectGuardian"
    />

    <aside
      class="mt-xl flex w-full items-center gap-md rounded-medium bg-disabled/40 p-lg text-left"
    >
      <span
        class="flex size-[48px] shrink-0 items-center justify-center rounded-full bg-surface-card text-body-muted"
        aria-hidden="true"
      >
        <Smartphone class="size-xl" />
      </span>
      <p class="type-body text-body-secondary">
        보호자의 폰에 표시된<br />
        <strong class="text-primary-500">5자리 숫자를 확인하세요.</strong>
      </p>
    </aside>
  </div>
</template>
