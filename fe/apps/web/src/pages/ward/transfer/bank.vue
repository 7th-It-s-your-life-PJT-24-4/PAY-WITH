<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const selectedBank = ref('')
const banks = [
  'KB국민은행',
  '우리은행',
  '하나은행',
  'NH농협',
  '신한은행',
  '카카오뱅크',
]

function proceed() {
  transferStore.selectManualRecipient(selectedBank.value)
  router.push({ name: 'ward-transfer-amount' })
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
        @click="selectedBank = bankName"
      >
        <span
          class="flex size-12 items-center justify-center rounded-full bg-gray-900 text-primary-300"
          >₩</span
        >
        {{ bankName }}
      </button>
    </section>

    <p class="type-h2 text-center">1/2</p>

    <Button
      class="w-full"
      label="다음으로"
      size="large"
      :disabled="!selectedBank"
      @click="proceed"
    />
  </div>
</template>
