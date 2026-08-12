<script setup lang="ts">
import { ChevronDown, ChevronLeft } from '@lucide/vue'
import { BottomSheet, Button, PinKeypad } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed, nextTick, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getApiErrorMessage } from '@/api/error'
import { useRegisterChargeAccountMutation } from '@/composables/useRegisterChargeAccountMutation'
import { banksOptions } from '@/lib/query/bank'
import GuardBankIconTile from '@/pages/guard/charge/-components/GuardBankIconTile.vue'
import GuardBankSelectBottomSheet from '@/pages/guard/charge/-components/GuardBankSelectBottomSheet.vue'
import { withGuardWardId } from '@/pages/guard/-utils/guard-route'
import type { Bank } from '@/schemas/bank.schema'
import { useGuardStore } from '@/stores/guard.store'
import { getBankPresentation } from '@/utils/bank-presentation'

const router = useRouter()
const guardStore = useGuardStore()
const isBankSheetOpen = ref(false)
const banksQuery = useQuery(banksOptions())
const registerAccountMutation = useRegisterChargeAccountMutation()
const selectedBank = ref<Bank | null>(null)
const accountNumber = ref('')
const accountPassword = ref('')
const keypad = ref<{ reset: () => void } | null>(null)
const isPasswordSheetOpen = ref(false)
const errorMessage = ref('')
const banks = computed(() => banksQuery.data.value ?? [])
const selectedBankPresentation = computed(() =>
  selectedBank.value ? getBankPresentation(selectedBank.value) : null,
)

const canConnect = computed(
  () =>
    Boolean(selectedBank.value) &&
    /^\d{8,16}$/.test(accountNumber.value) &&
    /^\d{4}$/.test(accountPassword.value) &&
    !registerAccountMutation.isPending.value,
)

function selectBank(bank: Bank) {
  selectedBank.value = bank
  isBankSheetOpen.value = false
}

function updateAccountNumber(value: string) {
  accountNumber.value = value.replace(/\D/g, '').slice(0, 16)
}

function openPasswordSheet() {
  accountPassword.value = ''
  isPasswordSheetOpen.value = true
  nextTick(() => keypad.value?.reset())
}

function closePasswordSheet() {
  accountPassword.value = ''
  keypad.value?.reset()
  isPasswordSheetOpen.value = false
}

function completePassword(value: string) {
  accountPassword.value = value
  isPasswordSheetOpen.value = false
}

function handlePasswordSheetOpenChange(open: boolean) {
  if (!open && accountPassword.value.length < 4) closePasswordSheet()
}

async function connectAccount() {
  const bank = selectedBank.value
  if (!canConnect.value || !bank) return
  errorMessage.value = ''

  try {
    const account = await registerAccountMutation.mutateAsync({
      bankCode: bank.bankCode,
      accountNo: accountNumber.value,
      accountPassword: accountPassword.value,
    })
    guardStore.selectChargeAccount(account.accountId)
    accountPassword.value = ''
    keypad.value?.reset()
    await router.replace({
      name: 'guard-charge-be',
      query: withGuardWardId({}, guardStore.activeWardId),
    })
  } catch (error) {
    errorMessage.value = await getApiErrorMessage(
      error,
      '계좌를 연동하지 못했습니다.',
    )
  }
}
</script>

<template>
  <main
    class="flex min-h-screen flex-col bg-white pb-[calc(96px+env(safe-area-inset-bottom))]"
  >
    <header class="grid h-11 grid-cols-[44px_1fr_44px] items-center">
      <button
        class="flex size-11 items-center justify-center text-gray-800"
        type="button"
        aria-label="뒤로 가기"
        @click="router.back()"
      >
        <ChevronLeft class="size-6" aria-hidden="true" />
      </button>
      <h1
        class="text-center text-[20px] font-semibold leading-[1.2] tracking-[-0.4px] text-black"
      >
        충전계좌 연결
      </h1>
    </header>

    <section class="flex-1 px-mobile-gutter pt-lg">
      <div>
        <label
          class="block text-[16px] font-medium leading-[1.2] tracking-[-0.32px] text-gray-600"
        >
          은행/증권사
        </label>
        <button
          class="mt-xs flex h-[60px] w-full items-center rounded-[14px] bg-[#F0F3F8] px-md text-left"
          type="button"
          @click="isBankSheetOpen = true"
        >
          <GuardBankIconTile
            v-if="selectedBank && selectedBankPresentation"
            class="mr-sm"
            :icon-url="selectedBankPresentation.iconUrl"
            :label="selectedBank.bankName"
            :brand-class="selectedBankPresentation.brandClass"
          >
            <span class="text-[13px] font-bold text-white">
              {{ selectedBank.bankName.slice(0, 1) }}
            </span>
          </GuardBankIconTile>
          <span
            class="flex-1 text-[18px] font-semibold leading-[1.2] tracking-[-0.36px]"
            :class="selectedBank ? 'text-black' : 'text-gray-700'"
          >
            {{ selectedBank?.bankName ?? '은행/증권사를 선택해주세요' }}
          </span>
          <ChevronDown
            class="size-6 shrink-0 text-gray-600"
            aria-hidden="true"
          />
        </button>
      </div>

      <label class="mt-md block">
        <span
          class="block text-[16px] font-medium leading-[1.2] tracking-[-0.32px] text-gray-600"
        >
          계좌번호
        </span>
        <input
          class="mt-xs h-[60px] w-full rounded-[14px] bg-[#F0F3F8] px-md text-[18px] font-semibold leading-[1.2] tracking-[-0.36px] text-black outline-none placeholder:text-gray-700"
          inputmode="numeric"
          placeholder="계좌번호를 입력해주세요"
          :value="accountNumber"
          @input="
            updateAccountNumber(
              ($event.target as { value: string } | null)?.value ?? '',
            )
          "
        />
      </label>

      <label class="mt-md block">
        <span
          class="block text-[16px] font-medium leading-[1.2] tracking-[-0.32px] text-gray-600"
        >
          계좌 비밀번호
        </span>
        <input
          class="mt-xs h-[60px] w-full cursor-pointer rounded-[14px] bg-[#F0F3F8] px-md text-center text-[18px] font-semibold leading-[1.2] tracking-[0.75em] text-primary-500 outline-none placeholder:tracking-[-0.36px] placeholder:text-gray-700 focus:ring-2 focus:ring-focus/20"
          inputmode="none"
          placeholder="4자리"
          type="text"
          :value="accountPassword ? '●●●●' : ''"
          readonly
          aria-haspopup="dialog"
          :aria-expanded="isPasswordSheetOpen"
          @click="openPasswordSheet"
          @keydown.enter.prevent="openPasswordSheet"
          @keydown.space.prevent="openPasswordSheet"
        />
      </label>

      <p
        v-if="errorMessage"
        class="mt-md text-center text-[14px] font-medium text-error"
        role="alert"
      >
        {{ errorMessage }}
      </p>
    </section>

    <div
      class="fixed inset-x-0 bottom-0 z-30 mx-auto w-full max-w-[390px] bg-white px-mobile-gutter pb-[calc(20px+env(safe-area-inset-bottom))] pt-sm"
    >
      <Button
        class="w-full"
        :label="
          registerAccountMutation.isPending.value
            ? '충전계좌 연결 중'
            : '충전계좌 연결하기'
        "
        variant="guard-cta"
        size="guard-cta"
        :disabled="!canConnect"
        @click="connectAccount"
      />
    </div>

    <GuardBankSelectBottomSheet
      v-model:open="isBankSheetOpen"
      :banks="banks"
      @select="selectBank"
    />

    <BottomSheet
      v-model:open="isPasswordSheetOpen"
      title="계좌 비밀번호 입력"
      description="보안을 위해 숫자 위치가 바뀔 수 있습니다."
      @update:open="handlePasswordSheetOpenChange"
    >
      <PinKeypad
        ref="keypad"
        class="[&_[role=group]]:!mt-xl"
        :length="4"
        variant="minimal"
        randomize
        pseudo-click
        :disabled="registerAccountMutation.isPending.value"
        @complete="completePassword"
        @change="accountPassword = ''"
        @cancel="closePasswordSheet"
      />
    </BottomSheet>
  </main>
</template>
