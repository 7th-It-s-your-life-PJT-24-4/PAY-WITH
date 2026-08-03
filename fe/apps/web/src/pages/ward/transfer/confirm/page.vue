<script setup lang="ts">
import { Button, Input } from '@pay-with/ui'
import { useRouter } from 'vue-router'

import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const formatMoney = (value: number) =>
  new Intl.NumberFormat('ko-KR').format(value)

function proceed() {
  if (!transferStore.createTransferIntent()) return
  router.push({ name: 'ward-transfer-password' })
}
</script>

<template>
  <div class="flex flex-col gap-xl">
    <h2 class="type-h1 text-center">
      <span class="font-number">{{ formatMoney(transferStore.amount) }}</span
      >원을 보낼까요?
    </h2>

    <section
      class="rounded-large border border-border bg-surface-card p-xl shadow-card"
    >
      <div class="text-center">
        <div
          class="type-h2 mx-auto flex size-20 items-center justify-center rounded-full bg-primary-900 text-primary-300"
        >
          {{ transferStore.recipient?.name.slice(0, 1) }}
        </div>
        <h3 class="type-h2 mt-md">{{ transferStore.recipient?.name }}</h3>
        <p class="type-h4 text-body-muted">
          {{ transferStore.bank }} {{ transferStore.accountNumber }}
        </p>
      </div>
      <dl class="mt-xl border-t border-border pt-md">
        <div class="flex justify-between py-xs">
          <dt>송금 금액</dt>
          <dd class="type-h2 text-primary-500">
            <span class="font-number">{{
              formatMoney(transferStore.amount)
            }}</span
            >원
          </dd>
        </div>
        <div class="flex justify-between py-xs">
          <dt>송금 후 잔액</dt>
          <dd>
            <span class="font-number">{{
              formatMoney(transferStore.remainingBalance)
            }}</span
            >원
          </dd>
        </div>
      </dl>
    </section>

    <Input
      v-model="transferStore.memo"
      label="메모"
      placeholder="메모 입력하기 (선택)"
      large
    />

    <Button
      class="mt-auto w-full"
      label="송금하기"
      size="large"
      @click="proceed"
    />
  </div>
</template>
