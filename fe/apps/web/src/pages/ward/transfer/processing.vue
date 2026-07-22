<script setup lang="ts">
import { Button } from '@pay-with/ui'
import { watch } from 'vue'
import { useRouter } from 'vue-router'

import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()

function retryPassword() {
  transferStore.restartAfterFailure()
  router.replace({ name: 'ward-transfer-password' })
}

watch(
  () => transferStore.processingStatus,
  (status) => {
    const transactionId = transferStore.transferResult?.transactionId
    if (status === 'success' && transactionId)
      router.replace({
        name: 'ward-transfer-complete',
        params: { transactionId },
      })
  },
  { immediate: true },
)
</script>

<template>
  <div
    class="flex min-h-[calc(100vh-var(--spacing-header)-var(--spacing-xl))] flex-col items-center justify-center text-center"
  >
    <template v-if="transferStore.processingStatus === 'pending'">
      <span
        class="size-20 animate-spin rounded-full border-[6px] border-primary-900 border-t-primary-500"
        aria-hidden="true"
      />
      <h2 class="type-h1 mt-xl">안전하게 송금하고 있습니다</h2>
      <p class="type-h3 mt-md text-body-muted" role="status" aria-live="polite">
        화면을 닫거나 뒤로 가지 말고<br />잠시만 기다려 주세요.
      </p>
    </template>

    <template v-else-if="transferStore.processingStatus === 'unknown'">
      <span
        class="type-h1 flex size-20 items-center justify-center rounded-full bg-primary-900 text-primary-500"
        aria-hidden="true"
        >?</span
      >
      <h2 class="type-h1 mt-xl">송금 결과를 확인하고 있습니다</h2>
      <p class="type-h3 mt-md text-body-muted">
        같은 송금을 다시 시도하지 마세요.
      </p>
      <Button
        class="mt-xl w-full"
        label="처리 결과 다시 확인"
        size="large"
        @click="transferStore.confirmMockStatus"
      />
    </template>

    <template v-else-if="transferStore.processingStatus === 'error'">
      <span
        class="type-h1 flex size-20 items-center justify-center rounded-full bg-error/10 text-error"
        aria-hidden="true"
        >!</span
      >
      <h2 class="type-h1 mt-xl">송금을 완료하지 못했습니다</h2>
      <p class="type-h3 mt-md text-body-muted">
        {{ transferStore.processingError }}
      </p>
      <Button
        class="mt-xl w-full"
        label="비밀번호 다시 입력"
        size="large"
        @click="retryPassword"
      />
    </template>
  </div>
</template>
