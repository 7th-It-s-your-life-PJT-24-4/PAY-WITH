<script setup lang="ts">
import { ArrowRight } from '@lucide/vue'
import { Button, Input, PinKeypad } from '@pay-with/ui'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'

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

function updateAccountNumber(value: string) {
  accountNumber.value = value.replace(/\D/g, '').slice(0, 16)
}

async function registerAccount() {
  if (!canRegister.value) return
  const account = await chargeStore.registerAccount({
    bankCode: bankCode.value,
    accountNumber: accountNumber.value,
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

    <Input
      :model-value="accountNumber"
      label="계좌번호"
      inputmode="numeric"
      placeholder="계좌번호를 입력해주세요"
      :error="accountNumberError"
      large
      numeric
      @update:model-value="updateAccountNumber"
    />

    <section aria-labelledby="account-password-title">
      <h3 id="account-password-title" class="type-h4">계좌 비밀번호</h3>
      <p class="type-body-medium mt-xs text-body-secondary">
        모의 은행 계좌 비밀번호 4자리를 입력해주세요.
      </p>
      <PinKeypad
        ref="keypad"
        class="mt-lg"
        :length="4"
        randomize
        pseudo-click
        :disabled="chargeStore.processingStatus === 'pending'"
        @complete="accountPassword = $event"
        @change="accountPassword = ''"
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
  </div>
</template>
