<script setup lang="ts">
import { ChevronLeft, Landmark, Wallet } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { useQuery } from '@tanstack/vue-query'
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { guardSafeAccountsOptions } from '@/lib/query/guard/safe-account'
import GuardBankIconTile from '@/pages/guard/charge/-components/GuardBankIconTile.vue'
import {
  parsePositiveRouteId,
  withGuardWardId,
} from '@/pages/guard/-utils/guard-route'
import { useGuardStore } from '@/stores/guard.store'
import { useSafeAccountStore } from '@/stores/safe-account.store'
import { getBankPresentation } from '@/utils/bank-presentation'

const route = useRoute()
const router = useRouter()
const guardStore = useGuardStore()
const safeAccountStore = useSafeAccountStore()
const routeWardId = computed(() => parsePositiveRouteId(route.query.wardId))
const wardId = computed(() => routeWardId.value ?? guardStore.activeWardId)
const safeAccountsQuery = useQuery(guardSafeAccountsOptions(wardId))
const safeAccounts = computed(() => safeAccountsQuery.data.value ?? [])

function goToAdd() {
  safeAccountStore.reset()
  router.push({
    name: 'guard-safe-account-add',
    query: { wardId: wardId.value ?? undefined },
  })
}

function goHome() {
  router.push({
    name: 'guard-home',
    query: withGuardWardId({}, wardId.value),
  })
}

function maskAccountNumber(accountNo: string) {
  const digits = accountNo.replace(/\D/g, '')
  return digits.length > 4 ? `•••• ${digits.slice(-4)}` : digits
}
</script>

<template>
  <main
    class="relative flex min-h-screen flex-col bg-white pb-[calc(84px+env(safe-area-inset-bottom))]"
  >
    <header class="grid h-11 grid-cols-[44px_1fr_44px] items-center">
      <button
        class="flex size-11 items-center justify-center text-[#3b3e43]"
        type="button"
        aria-label="보호자 홈으로 돌아가기"
        @click="goHome"
      >
        <ChevronLeft class="size-6" aria-hidden="true" />
      </button>
      <h1
        class="text-center text-[20px] font-bold leading-[1.2] tracking-[-0.4px] text-black"
      >
        안전계좌 목록
      </h1>
      <span aria-hidden="true" />
    </header>

    <section
      v-if="wardId === null"
      class="flex flex-1 flex-col items-center justify-center px-mobile-gutter text-center"
      role="alert"
    >
      <p class="text-[16px] font-medium leading-[1.5] text-gray-600">
        안전계좌를 확인할 시니어를<br />다시 선택해 주세요.
      </p>
      <Button
        class="mt-lg w-full"
        label="보호자 홈으로"
        variant="guard-cta"
        size="guard-cta"
        @click="goHome"
      />
    </section>

    <section
      v-else-if="safeAccountsQuery.isPending.value"
      class="flex flex-1 items-center justify-center px-mobile-gutter text-center text-[16px] font-medium text-gray-600"
      aria-busy="true"
    >
      안전계좌를 불러오는 중이에요.
    </section>

    <section
      v-else-if="safeAccountsQuery.isError.value"
      class="flex flex-1 flex-col items-center justify-center px-mobile-gutter text-center"
      role="alert"
    >
      <p class="text-[16px] font-medium leading-[1.5] text-gray-600">
        안전계좌를 불러오지 못했어요.<br />잠시 후 다시 시도해 주세요.
      </p>
      <Button
        class="mt-lg w-full"
        label="다시 시도"
        variant="outline-primary"
        @click="safeAccountsQuery.refetch()"
      />
    </section>

    <section
      v-else-if="safeAccounts.length === 0"
      class="flex flex-1 flex-col items-center justify-center px-mobile-gutter pb-11 text-center"
      aria-labelledby="safe-account-empty-title"
    >
      <span
        class="flex size-14 items-center justify-center rounded-full bg-[#f0f3f8] text-primary-500"
        aria-hidden="true"
      >
        <Landmark class="size-7" :stroke-width="2" />
      </span>
      <h2
        id="safe-account-empty-title"
        class="mt-lg text-[20px] font-bold leading-[1.3] tracking-[-0.4px] text-black"
      >
        등록된 안전계좌가 없어요
      </h2>
      <p
        class="mt-xs text-[15px] font-medium leading-[1.5] tracking-[-0.3px] text-gray-600"
      >
        자주 이용하는 계좌를 등록하면<br />더 안심하고 이용할 수 있어요.
      </p>
      <Button
        class="mt-xl w-full"
        label="안전계좌 추가"
        variant="guard-cta"
        size="guard-cta"
        @click="goToAdd"
      >
        <template #leading>
          <Wallet class="size-5" :stroke-width="2.2" />
        </template>
      </Button>
    </section>

    <section v-else class="px-mobile-gutter pt-xl" aria-label="등록된 안전계좌">
      <ul class="grid gap-md">
        <li
          v-for="account in safeAccounts"
          :key="account.safeAccountId"
          class="flex min-h-[84px] items-center rounded-large bg-[#f0f3f8] px-md py-sm"
        >
          <GuardBankIconTile
            class="!size-10 !rounded-[12px]"
            :icon-url="getBankPresentation(account).iconUrl"
            :brand-class="getBankPresentation(account).brandClass"
            :label="account.bankName"
            icon-class="max-h-7 max-w-7"
          >
            <Landmark class="size-5 text-white" aria-hidden="true" />
          </GuardBankIconTile>
          <div class="ml-md min-w-0 flex-1">
            <p
              class="truncate text-[17px] font-bold leading-[1.2] tracking-[-0.34px] text-black"
            >
              {{ account.accountAlias || account.holderName }}
            </p>
            <p
              class="mt-xs truncate text-[14px] font-medium leading-[1.2] tracking-[-0.28px] text-gray-600"
            >
              {{ account.bankName }} ·
              {{ maskAccountNumber(account.accountNo) }}
            </p>
          </div>
          <span
            class="rounded-small bg-[#d5ffd8] px-[6px] py-xxs text-[10px] font-bold leading-[1.2] tracking-[-0.2px] text-success"
          >
            안전
          </span>
        </li>
      </ul>
    </section>

    <div
      v-if="safeAccounts.length > 0"
      class="fixed inset-x-0 bottom-0 z-30 mx-auto flex w-full max-w-[390px] justify-end bg-gradient-to-t from-white via-white to-transparent px-mobile-gutter pb-[calc(20px+env(safe-area-inset-bottom))] pt-xl"
    >
      <Button
        class="!h-10 !min-h-10 !rounded-full !px-[10px] !text-[16px] !font-bold !leading-[1.6] !tracking-[-0.32px]"
        label="안전계좌 추가"
        variant="guard-cta"
        size="guard-cta"
        @click="goToAdd"
      >
        <template #leading>
          <Wallet class="size-5" :stroke-width="2.2" />
        </template>
      </Button>
    </div>
  </main>
</template>
