<script setup lang="ts">
import { Button, NumericKeypad } from '@pay-with/ui'
import { useMutation } from '@tanstack/vue-query'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { filterBanks } from '@/api/banks'
import { getApiErrorMessage } from '@/api/error'
import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const bankFilterMutation = useMutation({ mutationFn: filterBanks })
const errorMessage = ref('')

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
  <div
    class="flex min-h-[calc(100vh-var(--spacing-header)-var(--spacing-xl))] flex-col gap-xl"
  >
    <section class="text-center">
      <h2 class="type-h1">계좌 번호 입력</h2>
      <p class="type-h4 mt-xs text-body-secondary">
        보내실 분의 계좌 번호를 입력해 주세요
      </p>
    </section>

    <div
      class="flex h-[72px] shrink-0 items-center rounded-large border border-border-strong bg-surface-card px-xl"
    >
      <span
        v-if="transferStore.accountNumber"
        class="type-numeric-input-large font-number text-body"
      >
        {{ transferStore.accountNumber }}
      </span>
      <span v-else class="type-h3 text-body-muted">숫자만 입력</span>
    </div>

    <NumericKeypad
      class="mx-auto mt-auto"
      cancel-label=""
      @input="transferStore.appendAccountDigit"
      @backspace="transferStore.removeAccountDigit"
      @cancel="transferStore.accountNumber = ''"
    />

    <p
      v-if="errorMessage"
      class="type-body-medium text-center text-error"
      role="alert"
    >
      {{ errorMessage }}
    </p>

    <Button
      class="w-full"
      :label="bankFilterMutation.isPending.value ? '은행 찾는 중' : '다음으로'"
      size="large"
      :disabled="
        transferStore.accountNumber.length < 8 ||
        bankFilterMutation.isPending.value
      "
      @click="proceed"
    />
  </div>
</template>
