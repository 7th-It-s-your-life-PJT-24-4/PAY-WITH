<script setup lang="ts">
import { Button } from '@pay-with/ui'

defineProps<{
  phoneNumber: string
  phoneNumberError?: string
  phoneRequestError: string
  verificationCode: string
  verificationToken: string | null
  verificationMessage: string
  codeRequested: boolean
  codeTimerLabel: string
  isSending: boolean
  isVerifying: boolean
}>()

const emit = defineEmits<{
  'update:phoneNumber': [value: string]
  'update:verificationCode': [value: string]
  request: []
  confirm: []
}>()

function inputValueFromEvent(event: unknown) {
  if (typeof event !== 'object' || event === null) return ''

  const target = Reflect.get(event, 'target')
  if (typeof target !== 'object' || target === null) return ''

  const value = Reflect.get(target, 'value')
  return typeof value === 'string' ? value : ''
}
</script>

<template>
  <section class="flex flex-col gap-sm">
    <label class="type-h4 text-body" for="sign-up-phone">휴대폰 번호</label>
    <div class="flex gap-sm">
      <input
        id="sign-up-phone"
        :aria-describedby="phoneNumberError ? 'sign-up-phone-error' : undefined"
        :aria-invalid="phoneNumberError ? 'true' : undefined"
        :value="phoneNumber"
        autocomplete="tel"
        class="h-[52px] min-w-0 flex-1 rounded-medium border border-border-strong bg-surface-card px-md text-[16px] font-normal leading-[1.2] tracking-[-0.32px] text-body outline-none placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20"
        inputmode="tel"
        maxlength="13"
        pattern="01[016789]-?\d{3,4}-?\d{4}"
        placeholder="010-0000-0000"
        type="tel"
        @input="emit('update:phoneNumber', inputValueFromEvent($event))"
      />
      <Button
        :disabled="verificationToken !== null || isSending"
        :label="
          verificationToken
            ? '인증 완료'
            : isSending
              ? '발송 중'
              : codeRequested
                ? '재요청'
                : '인증하기'
        "
        type="button"
        variant="secondary"
        @click="emit('request')"
      />
    </div>
    <p
      v-if="phoneNumberError"
      id="sign-up-phone-error"
      class="type-caption text-error"
    >
      {{ phoneNumberError }}
    </p>
    <p v-if="phoneRequestError" class="type-caption text-error" role="alert">
      {{ phoneRequestError }}
    </p>
    <div v-if="codeRequested || verificationToken" class="flex flex-col gap-sm">
      <div class="flex gap-sm">
        <input
          :disabled="verificationToken !== null"
          :value="verificationCode"
          aria-label="휴대폰 인증번호"
          class="h-[52px] min-w-0 flex-1 rounded-medium border border-border-strong bg-surface-card px-md text-[16px] font-normal leading-[1.2] tracking-[-0.32px] text-body outline-none placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20"
          inputmode="numeric"
          maxlength="6"
          pattern="\d{6}"
          placeholder="인증번호 6자리"
          type="text"
          @input="emit('update:verificationCode', inputValueFromEvent($event))"
        />
        <Button
          v-if="verificationToken === null"
          :disabled="isVerifying"
          :label="isVerifying ? '확인 중' : '확인'"
          type="button"
          variant="secondary"
          @click="emit('confirm')"
        />
        <span v-else aria-hidden="true" class="h-[52px] w-[80px] shrink-0" />
      </div>
      <p
        v-if="verificationToken === null"
        class="type-caption flex items-center justify-between text-body-muted"
      >
        <span>인증번호 유효시간</span>
        <span class="font-semibold tabular-nums text-error">{{
          codeTimerLabel
        }}</span>
      </p>
      <p
        class="type-caption"
        :class="
          verificationMessage === '인증 완료했어요.'
            ? 'text-success'
            : 'text-body-muted'
        "
      >
        {{ verificationMessage }}
      </p>
    </div>
  </section>
</template>
