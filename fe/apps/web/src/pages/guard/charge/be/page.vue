<script setup lang="ts">
import { ChevronLeft, CircleAlert } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { chargeAccountsOptions } from '@/lib/query/account'
import GuardChargeAccountBottomSheet from '@/pages/guard/charge/-components/GuardChargeAccountBottomSheet.vue'
import GuardChargeAccountSelectCard from '@/pages/guard/charge/-components/GuardChargeAccountSelectCard.vue'
import { useGuardStore } from '@/stores/guard.store'

const router = useRouter()
const guardStore = useGuardStore()
const accountsQuery = useQuery(chargeAccountsOptions())
const amount = ref(0)
const isAccountSheetOpen = ref(false)

const amountText = computed(() =>
  amount.value > 0 ? new Intl.NumberFormat('ko-KR').format(amount.value) : '',
)
const accounts = computed(() => accountsQuery.data.value ?? [])
const selectedAccount = computed(() =>
  accounts.value.find(
    ({ accountId }) => accountId === guardStore.selectedChargeAccountId,
  ),
)
const canCharge = computed(
  () => amount.value > 0 && selectedAccount.value !== undefined,
)

watch(
  accounts,
  (value) => {
    if (value.length === 0) return
    if (
      !value.some(
        ({ accountId }) => accountId === guardStore.selectedChargeAccountId,
      )
    )
      guardStore.selectChargeAccount(value[0].accountId)
  },
  { immediate: true },
)

function updateAmount(value: string) {
  amount.value = Number(value.replace(/\D/g, ''))
}

function addAmount(value: number) {
  amount.value += value
}

function submitCharge() {
  if (!canCharge.value) return

  guardStore.setChargeAmount(amount.value)
  router.push({ name: 'guard-charge-password' })
}

function selectAccount(accountId: number) {
  guardStore.selectChargeAccount(accountId)
  isAccountSheetOpen.value = false
}

function goAccountAdd() {
  isAccountSheetOpen.value = false
  router.push({ name: 'guard-charge-account' })
}
</script>

<template>
  <main
    class="flex min-h-screen flex-col bg-white pb-[calc(96px+env(safe-area-inset-bottom))]"
  >
    <header class="grid h-11 grid-cols-[44px_1fr_44px] items-center">
      <button
        class="flex size-11 items-center justify-center text-gray-800"
        type="button"
        aria-label="뒤로 가기"
        @click="router.back()"
      >
        <ChevronLeft class="size-6" aria-hidden="true" />
      </button>
      <h1
        class="text-center text-[20px] font-semibold leading-[1.2] tracking-[-0.4px] text-black"
      >
        충전하기
      </h1>
    </header>

    <div class="flex-1 px-mobile-gutter pt-[56px]">
      <section aria-labelledby="guard-charge-form-title">
        <h2
          id="guard-charge-form-title"
          class="text-[20px] font-bold leading-[1.2] tracking-[-0.4px] text-black"
        >
          얼마나 충전할까요?
        </h2>

        <GuardChargeAccountSelectCard
          v-if="selectedAccount"
          class="mt-sm"
          :bank-name="selectedAccount.bankName"
          :account-suffix="selectedAccount.accountNo.slice(-4)"
          balance="연동 계좌"
          @click="isAccountSheetOpen = true"
        />

        <label
          class="mt-md block h-[74px] rounded-[12px] bg-[#F0F3F8] px-[14px] py-3"
        >
          <span
            class="block text-[12px] font-medium leading-[18px] tracking-[-0.2px] text-gray-800"
          >
            충전 금액
          </span>
          <span class="mt-xxs flex items-center">
            <input
              class="min-w-0 flex-1 bg-transparent text-xl font-semibold leading-7 tracking-[-0.22px] text-black outline-none placeholder:text-gray-700"
              inputmode="numeric"
              placeholder="얼마를 충전할까요?"
              :value="amountText"
              @input="
                updateAmount(
                  ($event.target as { value: string } | null)?.value ?? '',
                )
              "
            />
            <span
              v-if="amount > 0"
              class="ml-xs shrink-0 text-[20px] font-bold leading-7 text-black"
            >
              원
            </span>
          </span>
        </label>

        <div class="mt-xs flex flex-wrap gap-sm" aria-label="금액 빠른 입력">
          <button
            class="h-[33px] rounded-full bg-primary-500 px-md text-[14px] font-bold leading-[21px] tracking-[-0.2px] text-white"
            type="button"
            @click="addAmount(10_000)"
          >
            + 1만
          </button>
          <button
            class="h-[33px] rounded-full bg-primary-500 px-md text-[14px] font-bold leading-[21px] tracking-[-0.2px] text-white"
            type="button"
            @click="addAmount(50_000)"
          >
            + 5만
          </button>
          <button
            class="h-[33px] rounded-full bg-primary-500 px-md text-[14px] font-bold leading-[21px] tracking-[-0.2px] text-white"
            type="button"
            @click="addAmount(100_000)"
          >
            + 10만
          </button>
        </div>
      </section>
    </div>

    <div class="px-mobile-gutter">
      <aside
        class="flex min-h-[76px] items-center rounded-xl bg-[#F0F3F8] px-md py-sm text-gray-600"
      >
        <CircleAlert class="size-6 shrink-0" aria-hidden="true" />
        <p
          class="ml-md text-[16px] font-normal leading-[1.4] tracking-[-0.32px]"
        >
          충전된 금액은 즉시 지갑에 반영되며,<br />
          등록된 계좌에서 안전하게 출금됩니다.
        </p>
      </aside>
    </div>

    <div
      class="fixed inset-x-0 bottom-0 z-30 mx-auto w-full max-w-[390px] bg-white px-mobile-gutter pb-[calc(20px+env(safe-area-inset-bottom))] pt-sm"
    >
      <Button
        class="w-full"
        label="충전하기"
        variant="guard-cta"
        size="guard-cta"
        :disabled="!canCharge"
        @click="submitCharge"
      />
    </div>

    <GuardChargeAccountBottomSheet
      v-model:open="isAccountSheetOpen"
      :accounts="accounts"
      :selected-account-id="guardStore.selectedChargeAccountId"
      @select="selectAccount"
      @add="goAccountAdd"
    />
  </main>
</template>
