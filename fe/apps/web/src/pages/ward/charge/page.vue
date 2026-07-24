<script setup lang="ts">
import { ArrowRight, RotateCcw } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'

import ChargeAccountCard from '@/pages/ward/charge/-components/ChargeAccountCard.vue'
import { useChargeStore } from '@/stores/charge.store'

const router = useRouter()
const chargeStore = useChargeStore()
const formatMoney = (value: number) =>
  new Intl.NumberFormat('ko-KR').format(value)
const amountText = computed(() =>
  chargeStore.amount > 0 ? formatMoney(chargeStore.amount) : '',
)

onMounted(async () => {
  await chargeStore.loadAccounts()
  if (chargeStore.accounts.length === 0)
    await router.replace({ name: 'ward-charge-account-add' })
})

function updateAmount(value: string) {
  const digits = value.replace(/\D/g, '')
  chargeStore.setAmount(Number(digits))
}

async function submitCharge() {
  const result = await chargeStore.charge()
  if (!result) return
  await router.replace({
    name: 'ward-charge-complete',
    params: { transactionId: result.transactionId },
  })
}
</script>

<template>
  <div class="flex flex-col gap-xl">
    <section>
      <p class="type-body-medium text-body-secondary">출금 계좌</p>
      <ChargeAccountCard
        v-if="chargeStore.selectedAccount"
        class="mt-sm"
        :accounts="chargeStore.accounts"
        :selected-account-id="chargeStore.selectedAccount.accountId"
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
        label="+1만"
        size="small"
        variant="secondary"
        @click="chargeStore.addAmount(10_000)"
      />
      <Button
        label="+5만"
        size="small"
        variant="secondary"
        @click="chargeStore.addAmount(50_000)"
      />
      <Button
        label="+10만"
        size="small"
        variant="secondary"
        @click="chargeStore.addAmount(100_000)"
      />
      <Button
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
      v-if="chargeStore.processingError"
      class="type-body-medium text-center text-error"
      role="alert"
    >
      {{ chargeStore.processingError }}
    </p>

    <Button
      class="mt-auto w-full"
      :label="
        chargeStore.processingStatus === 'pending'
          ? '충전하고 있습니다'
          : '충전하기'
      "
      size="large"
      :disabled="!chargeStore.canCharge"
      @click="submitCharge"
    >
      <template #trailing>
        <ArrowRight />
      </template>
    </Button>
  </div>
</template>
