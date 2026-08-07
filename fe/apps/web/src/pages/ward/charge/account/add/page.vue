<script setup lang="ts">
import { ArrowRight } from '@lucide/vue'
import { Button, PinKeypad, WardToast } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getApiErrorMessage } from '@/api/error'
import { useEnsureFocusedInputVisible } from '@/composables/useEnsureFocusedInputVisible'
import { useRegisterChargeAccountMutation } from '@/composables/useRegisterChargeAccountMutation'
import { banksOptions } from '@/lib/query/bank'
import ChargeBankSelect from '@/pages/ward/charge/-components/ChargeBankSelect.vue'
import { useChargeStore } from '@/stores/charge.store'

const router = useRouter()
const chargeStore = useChargeStore()
const registerAccountMutation = useRegisterChargeAccountMutation()
const banksQuery = useQuery(banksOptions())

const banks = computed(() =>
  (banksQuery.data.value ?? []).map((b) => ({
    code: b.bankCode,
    name: b.bankName,
  })),
)
const bankCode = ref('')
const accountNumber = ref('')
const accountPassword = ref('')
const accountNumberInput = ref<HTMLElement | null>(null)
const keypad = ref<{ reset: () => void } | null>(null)
const passwordSheetOpen = ref(false)
const errorMessage = ref('')
const toastOpen = ref(false)
const toastMessage = ref('')

useEnsureFocusedInputVisible(accountNumberInput)

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
    !registerAccountMutation.isPending.value,
)

function handleAccountInput(event: Event) {
  const target = event.target as HTMLInputElement
  const digits = target.value.replace(/\D/g, '')
  accountNumber.value = digits.slice(0, 16)
  target.value = accountNumber.value
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

function handleRegisterClick() {
  if (!canRegister.value) {
    if (!bankCode.value) {
      toastMessage.value = '은행을 먼저 선택해 주세요'
    } else if (!/^\d{8,16}$/.test(accountNumber.value)) {
      toastMessage.value = '계좌번호 8자리 이상을 입력해 주세요'
    } else if (!/^\d{4}$/.test(accountPassword.value)) {
      toastMessage.value = '계좌 비밀번호 4자리를 입력해 주세요'
    } else {
      toastMessage.value = '입력 항목을 모두 확인해 주세요'
    }
    toastOpen.value = true
    return
  }
  void registerAccount()
}

async function registerAccount() {
  errorMessage.value = ''
  try {
    const account = await registerAccountMutation.mutateAsync({
      bankCode: bankCode.value,
      accountNo: accountNumber.value,
      accountPassword: accountPassword.value,
    })
    chargeStore.saveRegisteredAccount(account)
    accountPassword.value = ''
    keypad.value?.reset()
    await router.replace({ name: 'ward-charge-account-complete' })
  } catch (error) {
    errorMessage.value = await getApiErrorMessage(
      error,
      '계좌를 등록하지 못했습니다.',
    )
  }
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
        ref="accountNumberInput"
        class="h-[72px] w-full rounded-medium border bg-surface-card px-md text-body outline-none transition-colors placeholder:font-sans placeholder:text-[20px] placeholder:font-semibold placeholder:leading-none placeholder:tracking-[-0.4px] placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20"
        :class="[
          accountNumberError ? 'border-error' : 'border-border-strong',
          accountNumber
            ? 'type-numeric-input-large font-number leading-none'
            : '',
        ]"
        type="text"
        inputmode="numeric"
        placeholder="계좌번호를 입력해주세요"
        :value="accountNumber"
        :aria-invalid="accountNumberError ? 'true' : undefined"
        :aria-describedby="
          accountNumberError ? 'charge-account-number-error' : undefined
        "
        @input="handleAccountInput"
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
      v-if="errorMessage"
      class="type-body-medium text-center text-error"
      role="alert"
    >
      {{ errorMessage }}
    </p>

    <div @click="handleRegisterClick">
      <Button
        class="w-full"
        :class="{ 'opacity-50 cursor-not-allowed': !canRegister }"
        :label="
          registerAccountMutation.isPending.value
            ? '계좌를 등록하고 있습니다'
            : '계좌 등록하기'
        "
        size="large"
        :aria-disabled="!canRegister"
      >
        <template #trailing>
          <ArrowRight />
        </template>
      </Button>
    </div>

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
        :disabled="registerAccountMutation.isPending.value"
        cancel-label="닫기"
        @complete="completePassword"
        @change="accountPassword = ''"
        @cancel="closePasswordSheet"
      />
    </WardKeypadBottomSheet>

    <WardToast v-model:open="toastOpen" :message="toastMessage" />
  </div>
</template>
