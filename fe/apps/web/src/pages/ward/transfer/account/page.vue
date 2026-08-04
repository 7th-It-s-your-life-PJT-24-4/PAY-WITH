<script setup lang="ts">
import { Button, NumericKeypad } from '@pay-with/ui'
import { useRouter } from 'vue-router'

import { transferBanks } from '@/pages/ward/transfer/-utils/transfer-bank'
import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
function proceed() {
  transferStore.setBankCandidates(transferBanks.map(({ name }) => name))
  router.push({ name: 'ward-transfer-bank' })
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

    <Button
      class="w-full"
      label="다음으로"
      size="large"
      :disabled="transferStore.accountNumber.length < 8"
      @click="proceed"
    />
  </div>
</template>
