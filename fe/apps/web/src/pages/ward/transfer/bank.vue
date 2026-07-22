<script setup lang="ts">
import { Landmark } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { validateMockTransferAccount } from '@/mocks/transfer.mock'
import TransferErrorModal from '@/pages/ward/transfer/-components/TransferErrorModal.vue'
import TransferLoadingModal from '@/pages/ward/transfer/-components/TransferLoadingModal.vue'
import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const selectedBank = ref('')
const defaultBanks = [
  'KB국민은행',
  '우리은행',
  '하나은행',
  'NH농협',
  '신한은행',
  '카카오뱅크',
]
const banks = transferStore.bankCandidates.length
  ? transferStore.bankCandidates
  : defaultBanks
const isLoading = ref(false)
const errorMessage = ref('')

async function proceed() {
  if (!selectedBank.value || isLoading.value) return
  isLoading.value = true
  errorMessage.value = ''
  try {
    const account = await validateMockTransferAccount(
      selectedBank.value,
      transferStore.accountNumber,
    )
    transferStore.setVerifiedRecipient(account.recipientName, account.bank)
    await router.push({ name: 'ward-transfer-amount' })
  } catch (error) {
    errorMessage.value =
      error instanceof Error ? error.message : '계좌를 확인하지 못했습니다.'
  } finally {
    isLoading.value = false
  }
}
</script>

<template>
  <div class="flex flex-col gap-xl">
    <section class="text-center">
      <h2 class="type-h1">은행 선택</h2>
      <p class="type-h4 mt-xs text-body-secondary">
        거래하실 은행을 선택해 주세요.
      </p>
    </section>

    <Button
      class="w-full"
      label="계좌번호 다시 입력"
      variant="outline-primary"
      size="default"
      @click="router.push({ name: 'ward-transfer-account' })"
    />

    <section class="grid grid-cols-2 gap-md" aria-label="은행 목록">
      <button
        v-for="bankName in banks"
        :key="bankName"
        class="type-h4 flex h-28 flex-col items-center justify-center gap-sm rounded-large border bg-surface-card shadow-card"
        :class="
          selectedBank === bankName
            ? 'border-primary-500 ring-2 ring-primary-500/20'
            : 'border-border'
        "
        type="button"
        :disabled="isLoading"
        @click="selectedBank = bankName"
      >
        <span
          class="flex size-12 items-center justify-center rounded-full bg-gray-900 text-primary-300"
          aria-hidden="true"
        >
          <Landmark class="size-xl" :stroke-width="2.25" />
        </span>
        {{ bankName }}
      </button>
    </section>

    <p class="type-h2 text-center">1/2</p>

    <Button
      class="w-full"
      size="large"
      :label="isLoading ? '계좌 확인 중' : '다음으로'"
      :disabled="!selectedBank || isLoading"
      @click="proceed"
    />

    <TransferLoadingModal
      :open="isLoading"
      title="계좌를 확인하고 있습니다"
      description="선택한 은행과 계좌번호를 확인하고 있습니다."
    />
    <TransferErrorModal
      :open="Boolean(errorMessage)"
      :description="errorMessage"
      @retry="proceed"
      @cancel="errorMessage = ''"
    />
  </div>
</template>
