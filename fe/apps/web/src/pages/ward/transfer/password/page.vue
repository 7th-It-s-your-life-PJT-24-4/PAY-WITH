<script setup lang="ts">
import { PinKeypad } from '@pay-with/ui'
import { useRouter } from 'vue-router'

import { useCreateTransferMutation } from '@/composables/useCreateTransferMutation'
import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const transferMutation = useCreateTransferMutation()

function handleComplete(pin: string) {
  void transferStore.beginTransfer(pin, transferMutation.mutateAsync)
  router.replace({ name: 'ward-transfer-processing' })
}
</script>

<template>
  <div
    class="flex min-h-[calc(100vh-var(--spacing-header)-var(--spacing-xl))] flex-col"
  >
    <h2 class="type-h1 text-center">비밀번호를<br />입력해 주세요</h2>
    <section
      class="-mx-mobile-gutter mt-auto rounded-t-[32px] bg-surface-card px-xl pb-xl pt-16 shadow-[0_-8px_12px_rgb(0_0_0/4%)]"
      aria-label="비밀번호 키패드"
    >
      <PinKeypad @complete="handleComplete" />
    </section>
  </div>
</template>
