<script setup lang="ts">
import { Button, NumericKeypad } from '@pay-with/ui'
import { useRouter } from 'vue-router'

import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const formatMoney = (value: number) =>
  new Intl.NumberFormat('ko-KR').format(value)
</script>

<template>
  <div class="flex flex-col gap-lg">
    <section class="text-center">
      <div
        class="type-h2 mx-auto flex size-20 items-center justify-center rounded-full bg-primary-900 text-primary-300"
      >
        {{ transferStore.recipient?.name.slice(0, 1) }}
      </div>
      <h2 class="type-h2 mt-sm">{{ transferStore.recipient?.name }}</h2>
      <p class="type-body-medium text-body-muted">
        {{ transferStore.bank }} {{ transferStore.accountNumber }}
      </p>
    </section>

    <section
      class="rounded-large border-2 bg-surface-card p-xl text-center shadow-card"
      :class="
        transferStore.isAmountOverBalance
          ? 'border-error'
          : 'border-primary-500'
      "
    >
      <p
        class="type-amount"
        :class="
          transferStore.isAmountOverBalance ? 'text-error' : 'text-primary-500'
        "
      >
        <span class="font-number">{{ formatMoney(transferStore.amount) }}</span
        ><span class="type-h2 ml-xs">원</span>
      </p>
      <p
        class="type-h4 mt-md inline-block rounded-full px-md py-xs"
        :class="
          transferStore.isAmountOverBalance
            ? 'bg-error text-on-semantic'
            : 'bg-primary-900 text-primary-300'
        "
        :aria-label="
          transferStore.isAmountOverBalance
            ? `잔액 ${formatMoney(transferStore.balance)}원, 송금 가능 잔액 초과`
            : undefined
        "
      >
        잔액 {{ formatMoney(transferStore.balance) }}원
      </p>
    </section>

    <div class="grid grid-cols-4 gap-xs">
      <Button
        label="+5만원"
        size="small"
        variant="secondary"
        @click="transferStore.addAmount(50_000)"
      />
      <Button
        label="+10만원"
        size="small"
        variant="secondary"
        @click="transferStore.addAmount(100_000)"
      />
      <Button
        label="+50만원"
        size="small"
        variant="secondary"
        @click="transferStore.addAmount(500_000)"
      />
      <Button
        label="전액"
        size="small"
        variant="secondary"
        @click="transferStore.amount = transferStore.balance"
      />
    </div>

    <NumericKeypad
      class="mx-auto"
      @input="transferStore.appendAmountDigit"
      @backspace="transferStore.removeAmountDigit"
      @cancel="transferStore.amount = 0"
    />

    <Button
      class="w-full"
      label="다음으로"
      size="large"
      :disabled="!transferStore.canTransfer"
      @click="router.push({ name: 'ward-transfer-confirm' })"
    />
  </div>
</template>
