<script setup lang="ts">
import { CircleAlert, CircleQuestionMark, LoaderCircle } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { computed, watch } from 'vue'
import { useRouter } from 'vue-router'

import { createTransfer } from '@/api/transfers'
import { wardHomeOptions } from '@/lib/query/ward/home'
import { wardWalletOptions } from '@/lib/query/ward/wallet'
import type { WardHome } from '@/schemas/home.schema'
import type { CreateTransferRequest } from '@/schemas/transfer.schema'
import type { WalletBalance } from '@/schemas/wallet.schema'
import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const queryClient = useQueryClient()
const transferMutation = useMutation({
  mutationFn: ({
    request,
    idempotencyKey,
  }: {
    request: CreateTransferRequest
    idempotencyKey: string
  }) => createTransfer(request, idempotencyKey),
})

const failureButton = computed(() => {
  switch (transferStore.processingFailureAction) {
    case 'retry-pin':
      return {
        label: '비밀번호 다시 입력',
        routeName: 'ward-transfer-password',
      }
    case 'edit-account':
      return { label: '계좌번호 다시 입력', routeName: 'ward-transfer-account' }
    case 'charge':
      return { label: '충전하기', routeName: 'ward-charge' }
    case 'restart-transfer':
      return { label: '송금 처음부터', routeName: 'ward-transfer' }
    default:
      return { label: '홈으로', routeName: 'ward-home' }
  }
})
const showHomeButton = computed(
  () => failureButton.value.routeName !== 'ward-home',
)

function handleFailure() {
  const { routeName } = failureButton.value
  if (transferStore.processingFailureAction !== 'retry-pin')
    transferStore.reset()
  router.replace({ name: routeName })
}

function goHome() {
  transferStore.reset()
  router.replace({ name: 'ward-home' })
}

function confirmStatus() {
  void transferStore.confirmTransferStatus(transferMutation.mutateAsync)
}

watch(
  () => transferStore.processingStatus,
  (status) => {
    const transactionId = transferStore.transferResult?.transactionId
    if (status === 'success' && transactionId) {
      const balanceAfter = transferStore.transferDetail?.balanceAfter
      const currentWallet = queryClient.getQueryData<WalletBalance>(
        wardWalletOptions().queryKey,
      )
      if (balanceAfter !== null && balanceAfter !== undefined && currentWallet)
        queryClient.setQueryData(wardWalletOptions().queryKey, {
          ...currentWallet,
          balance: balanceAfter,
        })
      if (balanceAfter !== null && balanceAfter !== undefined)
        queryClient.setQueryData<WardHome>(
          wardHomeOptions().queryKey,
          (currentHome) =>
            currentHome
              ? {
                  ...currentHome,
                  wallet: { ...currentHome.wallet, balance: balanceAfter },
                }
              : currentHome,
        )
      void queryClient.invalidateQueries({
        queryKey: wardWalletOptions().queryKey,
      })
      void queryClient.invalidateQueries({
        queryKey: wardHomeOptions().queryKey,
      })
      router.replace({
        name: 'ward-transfer-complete',
        params: { transactionId },
      })
    }
    if (status === 'held' && transactionId)
      router.replace({
        name: 'ward-transfer-held',
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
      <LoaderCircle
        class="size-20 animate-spin text-primary-500"
        :stroke-width="2.5"
        aria-hidden="true"
      />
      <h2 class="type-h1 mt-xl">안전하게 송금하고 있습니다</h2>
      <p class="type-h3 mt-md text-body-muted" role="status" aria-live="polite">
        화면을 닫거나 뒤로 가지 말고<br />잠시만 기다려 주세요.
      </p>
    </template>

    <template v-else-if="transferStore.processingStatus === 'unknown'">
      <span
        class="flex size-20 items-center justify-center rounded-full bg-primary-900 text-primary-500"
        aria-hidden="true"
      >
        <CircleQuestionMark class="size-12" :stroke-width="2.25" />
      </span>
      <h2 class="type-h1 mt-xl">송금 결과를 확인하고 있습니다</h2>
      <p class="type-h3 mt-md text-body-muted">
        같은 송금을 다시 시도하지 마세요.
      </p>
      <Button
        class="mt-xl w-full"
        label="처리 결과 다시 확인"
        size="large"
        @click="confirmStatus"
      />
    </template>

    <template v-else-if="transferStore.processingStatus === 'error'">
      <span
        class="flex size-20 items-center justify-center rounded-full bg-error/10 text-error"
        aria-hidden="true"
      >
        <CircleAlert class="size-12" :stroke-width="2.25" />
      </span>
      <h2 class="type-h1 mt-xl">송금을 완료하지 못했습니다</h2>
      <p class="type-h3 mt-md text-body-muted">
        {{ transferStore.processingError }}
      </p>
      <div class="mt-xl flex w-full flex-col gap-md">
        <Button
          class="w-full"
          :label="failureButton.label"
          size="large"
          @click="handleFailure"
        />
        <Button
          v-if="showHomeButton"
          class="w-full"
          label="홈으로"
          variant="outline-primary"
          size="large"
          @click="goHome"
        />
      </div>
    </template>
  </div>
</template>
