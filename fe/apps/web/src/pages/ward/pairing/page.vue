<script setup lang="ts">
import { ShieldCheck, Smartphone } from '@lucide/vue'
import { Button, NumericKeypad } from '@pay-with/ui'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

import WardKeypadBottomSheet from '@/pages/ward/-components/WardKeypadBottomSheet.vue'
import { usePairingStore } from '@/stores/pairing.store'

const router = useRouter()
const pairingStore = usePairingStore()
const enteredCode = ref('')
const errorMessage = ref('')
const keypadOpen = ref(false)

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

async function connectGuardian() {
  if (!(await pairingStore.verifyCode(enteredCode.value))) {
    errorMessage.value =
      pairingStore.errorCode === 'PAIRING_004'
        ? '입력 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요.'
        : '인증 코드가 유효하지 않거나 만료되었습니다.'
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

    <button
      class="mt-xl flex w-full justify-center gap-sm rounded-medium outline-none focus-visible:ring-2 focus-visible:ring-focus focus-visible:ring-offset-4"
      type="button"
      aria-haspopup="dialog"
      :aria-expanded="keypadOpen"
      :aria-label="`인증 코드 ${enteredCode.length}자리 입력됨`"
      @focus="keypadOpen = true"
      @click="keypadOpen = true"
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
    </button>

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

    <WardKeypadBottomSheet
      v-model:open="keypadOpen"
      title="인증 코드 입력"
      description="보호자의 폰에 표시된 5자리 숫자를 입력해주세요."
    >
      <p
        class="type-numeric-input-large font-number min-h-12 text-center tracking-[0.35em] text-primary-500"
        aria-live="polite"
      >
        {{ enteredCode || '-----' }}
      </p>
      <NumericKeypad
        class="mt-lg"
        cancel-label="닫기"
        :disabled="pairingStore.isVerifyingCode"
        @input="inputDigit"
        @backspace="removeDigit"
        @cancel="keypadOpen = false"
      />
      <Button
        class="mt-lg w-full"
        label="입력 완료"
        size="large"
        :disabled="enteredCode.length !== 5"
        @click="keypadOpen = false"
      />
    </WardKeypadBottomSheet>
  </div>
</template>
