<script setup lang="ts">
import { ChevronDown, ChevronLeft } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { banksOptions } from '@/lib/query/bank'
import GuardBankSelectBottomSheet from '@/pages/guard/charge/-components/GuardBankSelectBottomSheet.vue'
import type { Bank } from '@/schemas/bank.schema'
import { useGuardStore } from '@/stores/guard.store'
import { useSafeAccountStore } from '@/stores/safe-account.store'

const route = useRoute()
const router = useRouter()
const guardStore = useGuardStore()
const safeAccountStore = useSafeAccountStore()
const isBankSheetOpen = ref(false)
const banksQuery = useQuery(banksOptions())
const banks = computed(() => banksQuery.data.value ?? [])
const routeWardId = computed(() => {
  const value = Number(route.query.wardId)
  return Number.isSafeInteger(value) && value > 0 ? value : null
})
const wardId = computed(() => routeWardId.value ?? guardStore.activeWardId)
const canContinue = computed(
  () =>
    /^\d{3}$/.test(safeAccountStore.bankCode) &&
    /^\d+$/.test(safeAccountStore.accountNumber),
)

function selectBank(bank: Bank) {
  safeAccountStore.selectBank({ code: bank.bankCode, name: bank.bankName })
  isBankSheetOpen.value = false
}

function updateAccountNumber(value: string) {
  safeAccountStore.setAccountNumber(value)
}

function goToList() {
  router.push({
    name: 'guard-safe-account',
    query: { wardId: wardId.value ?? undefined },
  })
}

function continueToConfirmation() {
  if (!canContinue.value) return
  router.push({
    name: 'guard-safe-account-confirm',
    query: { wardId: wardId.value ?? undefined },
  })
}
</script>

<template>
  <main
    class="flex min-h-screen flex-col bg-white pb-[calc(96px+env(safe-area-inset-bottom))]"
  >
    <header class="grid h-11 grid-cols-[44px_1fr_44px] items-center">
      <button
        class="flex size-11 items-center justify-center text-[#3b3e43]"
        type="button"
        aria-label="안전계좌 목록으로 돌아가기"
        @click="goToList"
      >
        <ChevronLeft class="size-6" aria-hidden="true" />
      </button>
      <h1
        class="text-center text-[20px] font-bold leading-[1.2] tracking-[-0.4px] text-black"
      >
        안전계좌 추가
      </h1>
      <span aria-hidden="true" />
    </header>

    <section class="flex-1 px-mobile-gutter pt-xl" aria-label="안전계좌 정보">
      <div>
        <label
          class="block text-[16px] font-medium leading-[1.2] tracking-[-0.32px] text-gray-600"
        >
          은행/증권사
        </label>
        <button
          class="mt-[2px] flex h-[62px] w-full items-center rounded-large bg-[#f0f3f8] px-md text-left"
          type="button"
          aria-haspopup="dialog"
          :aria-expanded="isBankSheetOpen"
          @click="isBankSheetOpen = true"
        >
          <span
            class="flex-1 text-[18px] font-semibold leading-[1.2] tracking-[-0.36px] text-black"
            :class="{ 'text-gray-700': !safeAccountStore.bankName }"
          >
            {{ safeAccountStore.bankName || '은행/증권사를 선택해 주세요' }}
          </span>
          <ChevronDown
            class="size-6 shrink-0 text-gray-600"
            aria-hidden="true"
          />
        </button>
      </div>

      <label class="mt-md block">
        <span
          class="block text-[16px] font-medium leading-[1.2] tracking-[-0.32px] text-gray-600"
        >
          계좌번호
        </span>
        <input
          class="mt-[2px] h-[62px] w-full rounded-large bg-[#f0f3f8] px-md text-[18px] font-semibold leading-[1.2] tracking-[-0.36px] text-black outline-none placeholder:text-gray-700"
          inputmode="numeric"
          autocomplete="off"
          aria-label="계좌번호"
          :value="safeAccountStore.accountNumber"
          @input="
            updateAccountNumber(
              ($event.target as { value: string } | null)?.value ?? '',
            )
          "
        />
      </label>
    </section>

    <div
      class="fixed inset-x-0 bottom-0 z-30 mx-auto w-full max-w-[390px] bg-white px-mobile-gutter pb-[calc(20px+env(safe-area-inset-bottom))] pt-sm"
    >
      <Button
        class="w-full"
        label="안전계좌 추가하기"
        variant="guard-cta"
        size="guard-cta"
        :disabled="!canContinue"
        @click="continueToConfirmation"
      />
    </div>

    <GuardBankSelectBottomSheet
      v-model:open="isBankSheetOpen"
      :banks="banks"
      @select="selectBank"
    />
  </main>
</template>
