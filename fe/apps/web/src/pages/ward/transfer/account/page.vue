<script setup lang="ts">
import { Button, WardToast } from '@pay-with/ui'
import { useMutation } from '@tanstack/vue-query'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { filterBanks } from '@/api/banks'
import { getApiErrorMessage } from '@/api/error'
import { useEnsureFocusedInputVisible } from '@/composables/useEnsureFocusedInputVisible'
import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const bankFilterMutation = useMutation({ mutationFn: filterBanks })
const accountNumberInput = ref<HTMLElement | null>(null)
const errorMessage = ref('')
const toastOpen = ref(false)
const toastMessage = ref('')

useEnsureFocusedInputVisible(accountNumberInput)

function handleAccountInput(event: Event) {
  const target = event.target as HTMLInputElement
  const digits = target.value.replace(/\D/g, '')
  transferStore.accountNumber = digits.slice(0, 16)
  target.value = transferStore.accountNumber
}

function handleProceedClick() {
  if (transferStore.accountNumber.length < 8) {
    toastMessage.value = '계좌번호 8자리 이상을 입력해 주세요'
    toastOpen.value = true
    return
  }
  void proceed()
}

async function proceed() {
  if (bankFilterMutation.isPending.value) return
  errorMessage.value = ''

  try {
    const banks = await bankFilterMutation.mutateAsync({
      accountNo: transferStore.accountNumber,
    })
    transferStore.setRecommendedBanks(banks)
    await router.push({ name: 'ward-transfer-bank' })
  } catch (error) {
    errorMessage.value = await getApiErrorMessage(
      error,
      '은행 정보를 불러오지 못했습니다. 다시 시도해 주세요.',
    )
  }
}
</script>

<template>
  <div class="flex flex-col gap-xl">
    <section class="text-center">
      <h2 class="type-h1">계좌 번호 입력</h2>
      <p class="type-h4 mt-xs text-body-secondary">
        보내실 분의 계좌 번호를 입력해 주세요
      </p>
    </section>

    <div class="flex flex-col gap-xs">
      <label for="transfer-account-number" class="sr-only">계좌 번호</label>
      <input
        id="transfer-account-number"
        ref="accountNumberInput"
        class="h-[72px] w-full rounded-medium border border-border-strong bg-surface-card px-md text-body outline-none transition-colors placeholder:font-sans placeholder:text-[20px] placeholder:font-semibold placeholder:leading-none placeholder:tracking-[-0.4px] placeholder:text-body-muted focus:border-focus focus:ring-2 focus:ring-focus/20"
        :class="[
          transferStore.accountNumber
            ? 'type-numeric-input-large font-number leading-none'
            : '',
        ]"
        type="text"
        inputmode="numeric"
        placeholder="숫자만 입력"
        :value="transferStore.accountNumber"
        @input="handleAccountInput"
      />
    </div>

    <p
      v-if="errorMessage"
      class="type-body-medium text-center text-error"
      role="alert"
    >
      {{ errorMessage }}
    </p>

    <div @click="handleProceedClick">
      <Button
        class="w-full"
        :class="{
          'opacity-50 cursor-not-allowed':
            transferStore.accountNumber.length < 8,
        }"
        :label="
          bankFilterMutation.isPending.value ? '은행 찾는 중' : '다음으로'
        "
        size="large"
        :aria-disabled="
          transferStore.accountNumber.length < 8 ||
          bankFilterMutation.isPending.value
        "
      />
    </div>

    <WardToast v-model:open="toastOpen" :message="toastMessage" />
  </div>
</template>
