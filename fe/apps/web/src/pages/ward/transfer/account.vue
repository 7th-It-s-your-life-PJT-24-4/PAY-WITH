<script setup lang="ts">
import { Button, NumericKeypad } from '@pay-with/ui'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { findMockBankCandidates } from '@/mocks/transfer.mock'
import TransferErrorModal from '@/pages/ward/transfer/-components/TransferErrorModal.vue'
import TransferLoadingModal from '@/pages/ward/transfer/-components/TransferLoadingModal.vue'
import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const isLoading = ref(false)
const errorMessage = ref('')

async function proceed() {
  if (isLoading.value) return
  isLoading.value = true
  errorMessage.value = ''
  try {
    transferStore.setBankCandidates(
      await findMockBankCandidates(transferStore.accountNumber),
    )
    await router.push({ name: 'ward-transfer-bank' })
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '은행을 찾지 못했습니다.'
  } finally {
    isLoading.value = false
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
      :disabled="isLoading"
      @input="transferStore.appendAccountDigit"
      @backspace="transferStore.removeAccountDigit"
      @cancel="transferStore.accountNumber = ''"
    />

    <Button
      class="w-full"
      :label="isLoading ? '은행 확인 중' : '다음으로'"
      size="large"
      :disabled="transferStore.accountNumber.length < 8 || isLoading"
      @click="proceed"
    />

    <TransferLoadingModal
      :open="isLoading"
      title="은행을 찾고 있습니다"
      description="입력한 계좌번호를 확인하고 있습니다."
    />
    <TransferErrorModal
      :open="Boolean(errorMessage)"
      :description="errorMessage"
      @retry="proceed"
      @cancel="errorMessage = ''"
    />
  </div>
</template>
