<script setup lang="ts">
import { Button, WardToast } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { useEnsureFocusedInputVisible } from '@/composables/useEnsureFocusedInputVisible'
import { wardWalletOptions } from '@/lib/query/ward/wallet'
import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const walletQuery = useQuery(wardWalletOptions())
const toastOpen = ref(false)
const toastMessage = ref('')
const amountInput = ref<HTMLInputElement | null>(null)

useEnsureFocusedInputVisible(amountInput)

const formatMoney = (value: number) =>
  new Intl.NumberFormat('ko-KR').format(value)

const maxAmountDigits = computed(() => {
  if (transferStore.balance === null) return null
  return String(Math.max(0, Math.floor(transferStore.balance))).length + 1
})

const maxInputAmount = computed(() => {
  if (maxAmountDigits.value === null) return null
  return 10 ** maxAmountDigits.value - 1
})

watch(
  () => walletQuery.data.value?.balance,
  (balance) => {
    if (balance !== undefined) transferStore.setBalance(balance)
  },
  { immediate: true },
)

function setCursorToEnd(el: HTMLInputElement | null) {
  if (!el) return
  nextTick(() => {
    const len = el.value.length
    try {
      el.setSelectionRange(len, len)
    } catch {
      // ignore
    }
  })
}

function focusAmountInput() {
  amountInput.value?.focus()
  setCursorToEnd(amountInput.value)
}

function handleAmountInput(event: Event) {
  const target = event.target as HTMLInputElement
  const normalizedDigits = target.value.replace(/\D/g, '')
  const digits =
    maxAmountDigits.value === null
      ? normalizedDigits
      : normalizedDigits.slice(0, maxAmountDigits.value)
  const num = digits ? Number(digits) : 0
  transferStore.amount = num
  target.value = num ? String(num) : ''
  setCursorToEnd(target)
}

function addAmount(value: number) {
  const next = transferStore.amount + value
  transferStore.amount = Math.min(next, maxInputAmount.value ?? next)
}

function handleCursorFix(event: Event) {
  const target = event.target as HTMLInputElement
  setCursorToEnd(target)
}

function handleNextClick() {
  if (!transferStore.canTransfer) {
    if (transferStore.amount <= 0) {
      toastMessage.value = '보낼 금액을 1원 이상 입력해 주세요'
    } else if (transferStore.isAmountOverBalance) {
      toastMessage.value = '지갑 잔액보다 큰 금액은 송금할 수 없어요'
    } else {
      toastMessage.value = '송금 금액을 다시 확인해 주세요'
    }
    toastOpen.value = true
    return
  }
  router.push({ name: 'ward-transfer-confirm' })
}

const quickAmountButtonClass =
  '!border-gray-900 !bg-surface-card whitespace-nowrap hover:!border-gray-900 hover:!bg-surface-card'
</script>

<template>
  <div class="flex flex-col gap-lg">
    <section class="text-center pt-xs">
      <h2 class="type-h1 text-body">{{ transferStore.recipient?.name }}</h2>
      <p class="type-h4 mt-xs text-body-secondary">
        {{ transferStore.bank }} {{ transferStore.accountNumber }}
      </p>
    </section>

    <section
      class="relative rounded-large border-2 bg-surface-card p-xl text-center shadow-card cursor-pointer"
      :class="
        transferStore.isAmountOverBalance
          ? 'border-error'
          : 'border-primary-500'
      "
      @click="focusAmountInput"
    >
      <input
        ref="amountInput"
        type="text"
        inputmode="numeric"
        class="absolute inset-0 size-full opacity-0 cursor-pointer"
        :value="transferStore.amount ? String(transferStore.amount) : ''"
        aria-label="송금 금액 입력"
        @input="handleAmountInput"
        @click="handleCursorFix"
        @focus="handleCursorFix"
        @select="handleCursorFix"
        @keyup="handleCursorFix"
      />
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
        :class="quickAmountButtonClass"
        label="+5만원"
        size="small"
        variant="secondary"
        @click="addAmount(50_000)"
      />
      <Button
        :class="quickAmountButtonClass"
        label="+10만원"
        size="small"
        variant="secondary"
        @click="addAmount(100_000)"
      />
      <Button
        :class="quickAmountButtonClass"
        label="+50만원"
        size="small"
        variant="secondary"
        @click="addAmount(500_000)"
      />
      <Button
        :class="quickAmountButtonClass"
        label="전액"
        size="small"
        variant="secondary"
        @click="transferStore.amount = transferStore.balance ?? 0"
      />
    </div>

    <div @click="handleNextClick">
      <Button
        class="w-full"
        :class="{ 'opacity-50 cursor-not-allowed': !transferStore.canTransfer }"
        label="다음으로"
        size="large"
        :aria-disabled="!transferStore.canTransfer"
      />
    </div>

    <WardToast v-model:open="toastOpen" :message="toastMessage" />
  </div>
</template>
