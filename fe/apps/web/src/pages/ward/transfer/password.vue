<script setup lang="ts">
import { NumericKeypad } from '@pay-with/ui'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

import { useTransferStore } from '@/stores/transfer.store'

const router = useRouter()
const transferStore = useTransferStore()
const pin = ref('')
const pinLength = 6

function input(value: string) {
  if (pin.value.length >= pinLength) return
  pin.value += value
  if (pin.value.length === pinLength) {
    void transferStore.beginMockTransfer(pin.value)
    router.push({ name: 'ward-transfer-processing' })
  }
}
</script>

<template>
  <div
    class="flex min-h-[calc(100vh-var(--spacing-header)-var(--spacing-xl))] flex-col"
  >
    <h2 class="type-h1 text-center">비밀번호를<br />입력해 주세요</h2>
    <div
      class="mt-16 flex justify-center gap-md"
      role="status"
      :aria-label="`비밀번호 ${pin.length}자리 입력됨`"
    >
      <span
        v-for="index in pinLength"
        :key="index"
        class="size-md rounded-full"
        :class="index <= pin.length ? 'bg-primary-500' : 'bg-gray-800'"
      />
    </div>
    <section
      class="-mx-mobile-gutter mt-auto rounded-t-[32px] bg-surface-card p-xl shadow-[0_-8px_12px_rgb(0_0_0/4%)]"
      aria-label="비밀번호 키패드"
    >
      <NumericKeypad
        class="mx-auto"
        cancel-label=""
        @input="input"
        @backspace="pin = pin.slice(0, -1)"
        @cancel="pin = ''"
      />
    </section>
  </div>
</template>
