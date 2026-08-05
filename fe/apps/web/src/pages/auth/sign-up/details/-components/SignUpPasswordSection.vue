<script setup lang="ts">
import { Eye, EyeOff } from '@lucide/vue'
import { ref, useId } from 'vue'

const props = defineProps<{
  loginPassword: string
  paymentPassword: string
  loginPasswordError?: string
  paymentPasswordError?: string
}>()

const emit = defineEmits<{
  'update:loginPassword': [value: string]
  'update:paymentPassword': [value: string]
}>()

const isPasswordVisible = ref(false)
const paymentPasswordInputIds = Array.from({ length: 6 }, () => useId())

function inputValueFromEvent(event: unknown) {
  if (typeof event !== 'object' || event === null) return ''

  const target = Reflect.get(event, 'target')
  if (typeof target !== 'object' || target === null) return ''

  const value = Reflect.get(target, 'value')
  return typeof value === 'string' ? value : ''
}

function updatePaymentPassword(index: number, value: string) {
  const digit = value.replace(/\D/g, '').slice(-1)
  const nextValue = props.paymentPassword.split('')

  nextValue[index] = digit
  emit('update:paymentPassword', nextValue.join('').slice(0, 6))

  if (digit && index < 5) {
    globalThis.document
      .getElementById(paymentPasswordInputIds[index + 1])
      ?.focus()
  }
}

function handlePaymentPasswordKeydown(
  index: number,
  event: globalThis.KeyboardEvent,
) {
  if (event.key !== 'Backspace' || index === 0) return

  event.preventDefault()
  if (paymentDigit(index)) updatePaymentPassword(index, '')
  globalThis.document
    .getElementById(paymentPasswordInputIds[index - 1])
    ?.focus()
}

function paymentDigit(index: number) {
  return props.paymentPassword[index] ?? ''
}
</script>

<template>
  <div class="flex flex-col gap-xl">
    <section class="flex flex-col gap-sm">
      <label class="type-h4 text-body" for="sign-up-password"
        >로그인 비밀번호</label
      >
      <div class="relative">
        <input
          id="sign-up-password"
          :aria-describedby="
            loginPasswordError ? 'sign-up-password-error' : undefined
          "
          :aria-invalid="loginPasswordError ? 'true' : undefined"
          :type="isPasswordVisible ? 'text' : 'password'"
          :value="loginPassword"
          autocomplete="new-password"
          class="h-[52px] w-full rounded-medium border border-border-strong bg-surface-card px-md pr-[52px] text-[16px] font-normal leading-[1.2] tracking-[-0.32px] text-body outline-none placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20"
          maxlength="64"
          minlength="8"
          placeholder="비밀번호를 입력하세요"
          @input="emit('update:loginPassword', inputValueFromEvent($event))"
        />
        <button
          :aria-label="isPasswordVisible ? '비밀번호 숨기기' : '비밀번호 보기'"
          class="absolute right-md top-1/2 -translate-y-1/2 text-body-muted focus-visible:outline-2 focus-visible:outline-focus"
          type="button"
          @click="isPasswordVisible = !isPasswordVisible"
        >
          <EyeOff v-if="isPasswordVisible" class="size-xl" />
          <Eye v-else class="size-xl" />
        </button>
      </div>
      <p
        id="sign-up-password-error"
        class="type-caption"
        :class="loginPasswordError ? 'text-error' : 'text-body-muted'"
      >
        {{ loginPasswordError ?? '숫자, 영문 포함 8자 이상' }}
      </p>
    </section>

    <section class="flex flex-col gap-sm">
      <label class="type-h4 text-body">결제 비밀번호 (6자리)</label>
      <div class="grid grid-cols-6 gap-sm">
        <input
          v-for="(_, index) in paymentPasswordInputIds"
          :id="paymentPasswordInputIds[index]"
          :key="paymentPasswordInputIds[index]"
          :aria-label="`결제 비밀번호 ${index + 1}번째 자리`"
          :value="paymentDigit(index)"
          class="type-h3 h-[52px] min-w-0 rounded-medium border border-border-strong bg-surface-card text-center text-body outline-none focus:border-focus focus:ring-2 focus:ring-focus/20"
          inputmode="numeric"
          maxlength="1"
          pattern="\d"
          type="password"
          @input="updatePaymentPassword(index, inputValueFromEvent($event))"
          @keydown="handlePaymentPasswordKeydown(index, $event)"
        />
      </div>
      <p
        class="type-caption"
        :class="paymentPasswordError ? 'text-error' : 'text-body-muted'"
      >
        {{
          paymentPasswordError ?? '결제 시 사용할 숫자 6자리를 입력해주세요.'
        }}
      </p>
    </section>
  </div>
</template>
