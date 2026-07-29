<script setup lang="ts">
import { ShieldCheck, Smartphone } from '@lucide/vue'
import { Button, NumericKeypad } from '@pay-with/ui'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import { usePairingStore } from '@/stores/pairing.store'

const router = useRouter()
const pairingStore = usePairingStore()
const enteredCode = ref('')
const errorMessage = ref('')

const codeDigits = computed(() =>
  Array.from({ length: 5 }, (_, index) => enteredCode.value[index] ?? ''),
)

function inputDigit(value: string) {
  if (enteredCode.value.length >= 5) return
  enteredCode.value += value
  errorMessage.value = ''
}

function removeDigit() {
  enteredCode.value = enteredCode.value.slice(0, -1)
  errorMessage.value = ''
}

function connectGuardian() {
  if (!pairingStore.verifyCode(enteredCode.value)) {
    errorMessage.value = '인증 코드를 확인해 주세요.'
    return
  }

  router.replace({ name: 'ward-pairing-complete' })
}
</script>

<template>
  <div class="flex flex-col items-center text-center">
    <span
      class="flex size-[64px] items-center justify-center rounded-full bg-primary-500/10 text-primary-500"
      aria-hidden="true"
    >
      <ShieldCheck class="size-8" :stroke-width="2" />
    </span>

    <h1 class="type-h1 mt-lg text-body">인증 코드 입력</h1>
    <p class="type-body mt-sm text-body-muted">
      보호자가 보내준 5자리 코드를 입력해 주세요.
    </p>

    <div
      class="mt-xl flex w-full justify-center gap-sm"
      role="status"
      :aria-label="`인증 코드 ${enteredCode.length}자리 입력됨`"
    >
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
      label="연결하기"
      :disabled="enteredCode.length !== 5"
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

    <NumericKeypad
      class="mt-xl"
      cancel-label=""
      @input="inputDigit"
      @backspace="removeDigit"
      @cancel="enteredCode = ''"
    />
  </div>
</template>
