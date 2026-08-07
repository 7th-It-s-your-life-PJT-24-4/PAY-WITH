<script setup lang="ts">
import { ChevronLeft } from '@lucide/vue'
import { PinKeypad } from '@pay-with/ui'
import { useMutation, useQueryClient } from '@tanstack/vue-query'
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { z } from 'zod'

import { createGuardCharge } from '@/api/guard-charges'
import { getApiErrorMessage } from '@/api/error'
import { guardChargeKeys } from '@/lib/query/guard/charge'
import { withGuardWardId } from '@/pages/guard/-utils/guard-route'
import { useGuardStore } from '@/stores/guard.store'

const router = useRouter()
const guardStore = useGuardStore()
const queryClient = useQueryClient()
const chargeMutation = useMutation({
  mutationFn: ({
    wardId,
    accountId,
    amount,
    pin,
  }: {
    wardId: number
    accountId: number
    amount: number
    pin: string
  }) => createGuardCharge(wardId, { accountId, amount, pin }),
})
const formRef = ref<{ requestSubmit: () => void } | null>(null)
const pin = ref('')
const errorMessage = ref('')

const passwordSchema = z.object({
  pin: z.string().regex(/^\d{6}$/, '간편 비밀번호 6자리를 입력해주세요.'),
})

const pinDigits = computed(() => pin.value.length)
const keypadOrder = ['1', '0', '4', '3', '2', '8', '5', '9', '7', '6']

async function submitPassword() {
  const validationResult = passwordSchema.safeParse({ pin: pin.value })
  if (!validationResult.success) {
    errorMessage.value =
      validationResult.error.issues[0]?.message ??
      '간편 비밀번호를 확인해주세요.'
    return
  }

  if (
    guardStore.activeWardId === null ||
    guardStore.selectedChargeAccountId === null ||
    !guardStore.canSubmitCharge
  ) {
    errorMessage.value = '충전 정보를 다시 확인해주세요.'
    return
  }

  try {
    const chargeResult = await chargeMutation.mutateAsync({
      wardId: guardStore.activeWardId,
      accountId: guardStore.selectedChargeAccountId,
      amount: guardStore.chargeAmount,
      pin: validationResult.data.pin,
    })
    guardStore.saveChargeResult(chargeResult)
    await queryClient.invalidateQueries({ queryKey: guardChargeKeys.all })
    errorMessage.value = ''
    await router.replace({
      name: 'guard-charge-complete',
      query: withGuardWardId({}, guardStore.activeWardId),
    })
  } catch (error) {
    errorMessage.value = await getApiErrorMessage(
      error,
      '충전을 완료하지 못했습니다. 다시 시도해주세요.',
    )
  }
}

function handleKeypadChange(length: number) {
  pin.value = pin.value.slice(0, length)
}

function handleKeypadComplete(value: string) {
  pin.value = value
}

watch(pinDigits, (length) => {
  if (length !== 6) return
  formRef.value?.requestSubmit()
})
</script>

<template>
  <main class="min-h-screen bg-white">
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
        간편 비밀번호
      </h1>
      <span aria-hidden="true" />
    </header>

    <form
      ref="formRef"
      class="px-mobile-gutter pt-[196px]"
      @submit.prevent="submitPassword"
    >
      <input
        class="sr-only"
        type="password"
        name="pin"
        autocomplete="current-password"
        inputmode="numeric"
        :value="pin"
        readonly
      />

      <PinKeypad
        variant="minimal"
        :key-order="keypadOrder"
        :randomize="false"
        :randomize-on-input="false"
        :disabled="chargeMutation.isPending.value"
        :error="errorMessage"
        @change="handleKeypadChange"
        @complete="handleKeypadComplete"
      />
    </form>
  </main>
</template>
