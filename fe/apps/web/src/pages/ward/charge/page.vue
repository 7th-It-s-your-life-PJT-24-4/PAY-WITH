<script setup lang="ts">
import { ArrowRight, RotateCcw } from '@lucide/vue'
import { Button, WardToast } from '@pay-with/ui'
import { useMutation, useQuery } from '@tanstack/vue-query'
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { createWardCharge } from '@/api/charges'
import { getApiErrorMessage } from '@/api/error'
import { chargeAccountsOptions } from '@/lib/query/account'
import ChargeAccountCard from '@/pages/ward/charge/-components/ChargeAccountCard.vue'
import { useChargeStore } from '@/stores/charge.store'

const router = useRouter()
const chargeStore = useChargeStore()

onMounted(() => {
  chargeStore.resetDraft()
})
const accountsQuery = useQuery(chargeAccountsOptions())
const chargeMutation = useMutation({ mutationFn: createWardCharge })
const errorMessage = ref('')
const toastOpen = ref(false)
const toastMessage = ref('')

const formatMoney = (value: number) =>
  new Intl.NumberFormat('ko-KR').format(value)
const amountText = computed(() =>
  chargeStore.amount > 0 ? formatMoney(chargeStore.amount) : '',
)
const accounts = computed(() => accountsQuery.data.value ?? [])
const selectedAccount = computed(
  () =>
    accounts.value.find(
      ({ accountId }) => accountId === chargeStore.selectedAccountId,
    ) ?? null,
)
const quickAmountButtonClass =
  '!border-gray-900 !bg-surface-card whitespace-nowrap hover:!border-gray-900 hover:!bg-surface-card'

watch(
  () => accountsQuery.data.value,
  async (value) => {
    if (!value) return
    chargeStore.syncAccounts(value)
    if (value.length === 0)
      await router.replace({ name: 'ward-charge-account-add' })
  },
  { immediate: true },
)

function updateAmount(value: string) {
  const digits = value.replace(/\D/g, '')
  chargeStore.setAmount(Number(digits))
}

function handleAmountBeforeInput(event: InputEvent) {
  if (
    event.inputType.startsWith('delete') ||
    event.inputType === 'historyUndo' ||
    event.inputType === 'historyRedo'
  )
    return

  if (event.data && /\D/.test(event.data)) event.preventDefault()
}

function handleAmountPaste(event: ClipboardEvent) {
  event.preventDefault()
  updateAmount(event.clipboardData?.getData('text') ?? '')
}

function handleSubmitChargeClick() {
  if (!selectedAccount.value) {
    toastMessage.value = '출금 계좌를 선택해 주세요'
    toastOpen.value = true
    return
  }
  if (!chargeStore.canCharge) {
    if (chargeStore.amount <= 0) {
      toastMessage.value = '충전할 금액을 1원 이상 입력해 주세요'
    } else {
      toastMessage.value = '충전 금액을 다시 확인해 주세요'
    }
    toastOpen.value = true
    return
  }
  void submitCharge()
}

async function submitCharge() {
  if (!selectedAccount.value || chargeMutation.isPending.value) return
  errorMessage.value = ''
  let result
  try {
    result = await chargeMutation.mutateAsync({
      accountId: selectedAccount.value.accountId,
      amount: chargeStore.amount,
    })
  } catch (error) {
    errorMessage.value = await getApiErrorMessage(
      error,
      '충전을 완료하지 못했습니다.',
    )
    return
  }

  chargeStore.saveResult(result)
  await router.replace({
    name: 'ward-charge-complete',
    params: { transactionId: result.transactionId },
  })
}
</script>

<template>
  <div
    v-if="accountsQuery.isPending.value"
    class="type-body-medium flex flex-1 items-center justify-center text-body-secondary"
    role="status"
  >
    계좌 정보를 불러오고 있습니다.
  </div>

  <div
    v-else-if="accountsQuery.isError.value"
    class="flex flex-1 flex-col items-center justify-center gap-lg text-center"
  >
    <p class="type-body-medium text-error" role="alert">
      계좌 정보를 불러오지 못했습니다.
    </p>
    <Button
      label="다시 시도"
      variant="outline-primary"
      @click="accountsQuery.refetch()"
    />
  </div>

  <div v-else-if="selectedAccount" class="flex flex-col gap-xl">
    <section>
      <p class="type-body-medium text-body-secondary">출금 계좌</p>
      <ChargeAccountCard
        class="mt-sm"
        :accounts="accounts"
        :selected-account-id="selectedAccount.accountId"
        @select="chargeStore.selectAccount"
        @add="router.push({ name: 'ward-charge-account-add' })"
      />
    </section>

    <section aria-labelledby="charge-amount-title">
      <h2 id="charge-amount-title" class="type-h2">
        충전할 금액을 입력해주세요
      </h2>
      <label
        class="mt-lg flex min-h-[80px] items-center border-b-2 border-primary-500"
      >
        <span class="sr-only">충전 금액</span>
        <input
          class="type-amount font-number min-w-0 flex-1 bg-transparent text-right text-primary-500 outline-none placeholder:font-sans placeholder:text-body-muted"
          inputmode="numeric"
          placeholder="0"
          :value="amountText"
          @beforeinput="handleAmountBeforeInput"
          @paste="handleAmountPaste"
          @input="
            updateAmount(
              ($event.target as { value: string } | null)?.value ?? '',
            )
          "
        />
        <span class="type-h2 ml-sm">원</span>
      </label>
    </section>

    <div class="grid grid-cols-4 gap-xs" aria-label="금액 빠른 입력">
      <Button
        :class="quickAmountButtonClass"
        label="+1만"
        size="small"
        variant="secondary"
        @click="chargeStore.addAmount(10_000)"
      />
      <Button
        :class="quickAmountButtonClass"
        label="+5만"
        size="small"
        variant="secondary"
        @click="chargeStore.addAmount(50_000)"
      />
      <Button
        :class="quickAmountButtonClass"
        label="+10만"
        size="small"
        variant="secondary"
        @click="chargeStore.addAmount(100_000)"
      />
      <Button
        :class="quickAmountButtonClass"
        label="초기화"
        size="small"
        variant="secondary"
        @click="chargeStore.setAmount(0)"
      >
        <template #leading>
          <RotateCcw />
        </template>
      </Button>
    </div>

    <p
      v-if="errorMessage"
      class="type-body-medium text-center text-error"
      role="alert"
    >
      {{ errorMessage }}
    </p>

    <div @click="handleSubmitChargeClick">
      <Button
        class="mt-auto w-full"
        :class="{
          'opacity-50 cursor-not-allowed':
            !chargeStore.canCharge || chargeMutation.isPending.value,
        }"
        :label="
          chargeMutation.isPending.value ? '충전하고 있습니다' : '충전하기'
        "
        size="large"
        :aria-disabled="
          !chargeStore.canCharge || chargeMutation.isPending.value
        "
      >
        <template #trailing>
          <ArrowRight />
        </template>
      </Button>
    </div>

    <WardToast v-model:open="toastOpen" :message="toastMessage" />
  </div>
</template>
