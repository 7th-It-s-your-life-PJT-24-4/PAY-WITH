<script setup lang="ts">
import { Button, NumericKeypad } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { watch } from 'vue'
import { useRouter } from 'vue-router'

import { wardWalletOptions } from '@/lib/query/ward/wallet'
import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const walletQuery = useQuery(wardWalletOptions())
const formatMoney = (value: number) =>
  new Intl.NumberFormat('ko-KR').format(value)

watch(
  () => walletQuery.data.value?.balance,
  (balance) => {
    if (balance !== undefined) transferStore.setBalance(balance)
  },
  { immediate: true },
)
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
            ? `잔액 ${formatMoney(transferStore.balance ?? 0)}원, 송금 가능 잔액 초과`
            : undefined
        "
      >
        <template v-if="transferStore.balance !== null">
          잔액 {{ formatMoney(transferStore.balance) }}원
        </template>
        <template v-else-if="walletQuery.isError.value">
          잔액을 불러오지 못했습니다
        </template>
        <template v-else>잔액 확인 중</template>
      </p>
    </section>

    <div v-if="walletQuery.isError.value" class="grid gap-sm text-center">
      <p class="type-body-medium text-error" role="alert">
        현재 잔액을 확인한 후 송금할 수 있습니다.
      </p>
      <Button
        label="잔액 다시 조회"
        variant="outline-primary"
        @click="walletQuery.refetch()"
      />
    </div>

    <div v-if="transferStore.balance !== null" class="grid grid-cols-4 gap-xs">
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
        @click="transferStore.amount = transferStore.balance ?? 0"
      />
    </div>

    <NumericKeypad
      v-if="transferStore.balance !== null"
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
