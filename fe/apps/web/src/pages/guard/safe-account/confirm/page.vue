<script setup lang="ts">
import { ChevronLeft } from '@lucide/vue'
import { Button } from '@pay-with/ui'
import { useMutation } from '@tanstack/vue-query'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { registerGuardSafeAccount } from '@/api/guard-safe-accounts'
import { getApiErrorMessage } from '@/api/error'
import { useGuardStore } from '@/stores/guard.store'
import { useSafeAccountStore } from '@/stores/safe-account.store'

const router = useRouter()
const guardStore = useGuardStore()
const safeAccountStore = useSafeAccountStore()
const errorMessage = ref('')
const registerMutation = useMutation({
  mutationFn: ({
    wardId,
    bankCode,
    accountNo,
  }: {
    wardId: number
    bankCode: string
    accountNo: string
  }) => registerGuardSafeAccount(wardId, { bankCode, accountNo }),
})

async function completeSafeAccount() {
  if (guardStore.activeWardId === null) {
    errorMessage.value = '연결할 시니어를 다시 선택해주세요.'
    return
  }

  try {
    await registerMutation.mutateAsync({
      wardId: guardStore.activeWardId,
      bankCode: safeAccountStore.bankCode,
      accountNo: safeAccountStore.accountNumber,
    })
    safeAccountStore.reset()
    await router.replace({ name: 'guard-home' })
  } catch (error) {
    errorMessage.value = await getApiErrorMessage(
      error,
      '안전계좌를 등록하지 못했습니다.',
    )
  }
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
        aria-label="뒤로 가기"
        @click="router.back()"
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

    <section
      class="flex flex-1 items-start justify-center px-mobile-gutter pt-[120px] text-center"
      aria-labelledby="safe-account-confirm-title"
    >
      <h2
        id="safe-account-confirm-title"
        class="text-[32px] font-bold leading-[1.4] tracking-[-0.64px] text-black"
      >
        입력한 계좌를<br />
        안전계좌에<br />
        추가할까요?
      </h2>
    </section>

    <p
      v-if="errorMessage"
      class="type-body-medium px-mobile-gutter text-center text-error"
      role="alert"
    >
      {{ errorMessage }}
    </p>

    <div
      class="fixed inset-x-0 bottom-0 z-30 mx-auto w-full max-w-[390px] bg-white px-mobile-gutter pb-[calc(20px+env(safe-area-inset-bottom))] pt-sm"
    >
      <Button
        class="w-full"
        :label="
          registerMutation.isPending.value
            ? '등록 중입니다'
            : '안전계좌 추가하기'
        "
        variant="guard-cta"
        size="guard-cta"
        :disabled="registerMutation.isPending.value"
        @click="completeSafeAccount"
      />
    </div>
  </main>
</template>
