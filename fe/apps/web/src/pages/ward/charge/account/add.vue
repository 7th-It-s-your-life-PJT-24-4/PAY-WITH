<script setup lang="ts">
import { ArrowRight } from '@lucide/vue'
import { Button, NumericKeypad, PinKeypad } from '@pay-with/ui'
import { computed, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'

import WardKeypadBottomSheet from '@/pages/ward/-components/WardKeypadBottomSheet.vue'
import ChargeBankSelect from '@/pages/ward/charge/-components/ChargeBankSelect.vue'
import { useChargeStore } from '@/stores/charge.store'

const banks = [
  { code: 'KB', name: 'KB국민은행' },
  { code: 'NH', name: '농협은행' },
  { code: 'SHINHAN', name: '신한은행' },
  { code: 'WOORI', name: '우리은행' },
  { code: 'HANA', name: '하나은행' },
]

const router = useRouter()
const chargeStore = useChargeStore()
const bankCode = ref('')
const accountNumber = ref('')
const accountPassword = ref('')
const keypad = ref<{ reset: () => void } | null>(null)
const accountSheetOpen = ref(false)
const passwordSheetOpen = ref(false)
const accountNumberError = computed(() =>
  accountNumber.value.length > 0 && !/^\d{8,16}$/.test(accountNumber.value)
    ? '계좌번호는 숫자 8~16자리로 입력해주세요.'
    : '',
)
const canRegister = computed(
  () =>
    bankCode.value !== '' &&
    /^\d{8,16}$/.test(accountNumber.value) &&
    /^\d{4}$/.test(accountPassword.value) &&
    chargeStore.processingStatus !== 'pending',
)

function appendAccountDigit(value: string) {
  if (accountNumber.value.length < 16) accountNumber.value += value
}

function removeAccountDigit() {
  accountNumber.value = accountNumber.value.slice(0, -1)
}

function openPasswordSheet() {
  accountPassword.value = ''
  passwordSheetOpen.value = true
  nextTick(() => keypad.value?.reset())
}

function closePasswordSheet() {
  accountPassword.value = ''
  keypad.value?.reset()
  passwordSheetOpen.value = false
}

function completePassword(value: string) {
  accountPassword.value = value
  passwordSheetOpen.value = false
}

async function registerAccount() {
  if (!canRegister.value) return
  const account = await chargeStore.registerAccount({
    bankCode: bankCode.value,
    accountNo: accountNumber.value,
    accountPassword: accountPassword.value,
  })
  accountPassword.value = ''
  keypad.value?.reset()
  if (account) await router.replace({ name: 'ward-charge-account-complete' })
}
</script>

<template>
  <div class="flex flex-col gap-xl">
    <section>
      <h2 class="type-h1">충전에 사용할 계좌를<br />등록해주세요</h2>
      <p class="type-body-medium mt-sm text-body-secondary">
        본인 명의의 은행 계좌를 등록할 수 있습니다.
      </p>
    </section>

    <div class="flex flex-col gap-xs">
      <span class="type-h4">은행</span>
      <ChargeBankSelect v-model="bankCode" :banks="banks" />
    </div>

    <div class="flex w-full flex-col gap-xs">
      <label for="charge-account-number" class="type-h4 text-body">
        계좌번호
      </label>
      <input
        id="charge-account-number"
        class="type-numeric-input-large font-number h-[72px] w-full cursor-pointer rounded-medium border bg-surface-card px-md text-body outline-none transition-colors placeholder:font-sans placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20"
        :class="accountNumberError ? 'border-error' : 'border-border-strong'"
        type="text"
        inputmode="none"
        placeholder="계좌번호를 입력해주세요"
        :value="accountNumber"
        readonly
        aria-haspopup="dialog"
        :aria-expanded="accountSheetOpen"
        :aria-invalid="accountNumberError ? 'true' : undefined"
        :aria-describedby="
          accountNumberError ? 'charge-account-number-error' : undefined
        "
        @focus="accountSheetOpen = true"
        @click="accountSheetOpen = true"
      />
      <p
        v-if="accountNumberError"
        id="charge-account-number-error"
        class="type-caption text-error"
      >
        {{ accountNumberError }}
      </p>
    </div>

    <section aria-labelledby="account-password-title">
      <h3 id="account-password-title" class="type-h4">계좌 비밀번호</h3>
      <p class="type-body-medium mt-xs text-body-secondary">
        모의 은행 계좌 비밀번호 4자리를 입력해주세요.
      </p>
      <input
        class="type-h3 mt-sm h-[72px] w-full cursor-pointer rounded-medium border border-border-strong bg-surface-card px-md text-center tracking-[0.75em] text-primary-500 outline-none transition-colors placeholder:tracking-normal placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20"
        type="text"
        inputmode="none"
        placeholder="비밀번호 4자리 입력"
        :value="accountPassword ? '●●●●' : ''"
        readonly
        aria-label="계좌 비밀번호"
        aria-haspopup="dialog"
        :aria-expanded="passwordSheetOpen"
        @focus="openPasswordSheet"
        @click="openPasswordSheet"
      />
    </section>

    <p
      v-if="chargeStore.processingError"
      class="type-body-medium text-center text-error"
      role="alert"
    >
      {{ chargeStore.processingError }}
    </p>

    <Button
      class="w-full"
      :label="
        chargeStore.processingStatus === 'pending'
          ? '계좌를 등록하고 있습니다'
          : '계좌 등록하기'
      "
      size="large"
      :disabled="!canRegister"
      @click="registerAccount"
    >
      <template #trailing>
        <ArrowRight />
      </template>
    </Button>

    <WardKeypadBottomSheet
      v-model:open="accountSheetOpen"
      title="계좌번호 입력"
      description="숫자 키패드로 계좌번호를 입력해주세요."
      mode="input"
    >
      <NumericKeypad
        cancel-label="닫기"
        @input="appendAccountDigit"
        @backspace="removeAccountDigit"
        @cancel="accountSheetOpen = false"
      />
      <Button
        class="mt-lg w-full"
        label="입력 완료"
        size="large"
        :disabled="!/^\d{8,16}$/.test(accountNumber)"
        @click="accountSheetOpen = false"
      />
    </WardKeypadBottomSheet>

    <WardKeypadBottomSheet
      v-model:open="passwordSheetOpen"
      title="계좌 비밀번호 입력"
      description="보안을 위해 숫자 위치가 바뀔 수 있습니다."
      :close-on-outside="false"
      @update:open="
        !$event && accountPassword.length < 4 && closePasswordSheet()
      "
    >
      <PinKeypad
        ref="keypad"
        :length="4"
        randomize
        pseudo-click
        :disabled="chargeStore.processingStatus === 'pending'"
        cancel-label="닫기"
        @complete="completePassword"
        @change="accountPassword = ''"
        @cancel="closePasswordSheet"
      />
    </WardKeypadBottomSheet>
  </div>
</template>
