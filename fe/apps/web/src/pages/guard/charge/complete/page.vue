<script setup lang="ts">
import { Check, ChevronLeft } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { computed } from 'vue'
import { useRouter } from 'vue-router'

import { useGuardStore } from '@/stores/guard.store'

const router = useRouter()
const guardStore = useGuardStore()

const seniorName = computed(
  () => guardStore.lastChargeResult?.wardName ?? '시니어',
)
const amountText = computed(() =>
  new Intl.NumberFormat('ko-KR').format(
    guardStore.chargeAmount > 0 ? guardStore.chargeAmount : 1,
  ),
)
</script>

<template>
  <main
    class="relative flex min-h-screen flex-col bg-white pb-[calc(96px+env(safe-area-inset-bottom))]"
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
      <span aria-hidden="true" />
      <span aria-hidden="true" />
    </header>

    <section
      class="flex flex-1 flex-col items-center px-mobile-gutter pt-[100px] text-center"
      aria-labelledby="guard-charge-complete-title"
    >
      <span
        class="flex size-20 items-center justify-center rounded-full bg-primary-500 text-white"
      >
        <Check class="size-12" stroke-width="4" aria-hidden="true" />
      </span>

      <h1
        id="guard-charge-complete-title"
        class="mt-xl text-[28px] font-semibold leading-[1.2] tracking-[-0.56px] text-black"
      >
        {{ seniorName }}님에게<br />
        {{ amountText }}원을<br />
        옮겼어요
      </h1>
    </section>

    <div
      class="fixed inset-x-0 bottom-0 z-30 mx-auto w-full max-w-[390px] bg-white px-mobile-gutter pb-[calc(20px+env(safe-area-inset-bottom))] pt-sm"
    >
      <Button
        class="w-full"
        label="확인"
        variant="guard-cta"
        size="guard-cta"
        @click="router.replace({ name: 'guard-charge' })"
      />
    </div>
  </main>
</template>
