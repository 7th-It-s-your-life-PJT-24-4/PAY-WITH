<script setup lang="ts">
import { ChevronRight, LockKeyhole, X } from '@lucide/vue'

defineProps<{
  count: number
  seniorName: string
}>()

const emit = defineEmits<{
  close: []
  confirm: []
}>()
</script>

<template>
  <section
    class="flex items-center gap-md bg-white py-lg"
    aria-labelledby="guard-risk-transaction-title"
  >
    <span
      class="flex size-10 shrink-0 items-center justify-center rounded-full bg-primary-900 text-primary-500"
      aria-hidden="true"
    >
      <LockKeyhole class="size-6" :stroke-width="2.5" />
    </span>

    <div class="min-w-0 flex-1">
      <p
        class="text-[18px] font-medium leading-[1.2] tracking-[-0.36px] text-gray-600"
      >
        {{ seniorName }}님의 송금·거래 중
      </p>
      <h2
        id="guard-risk-transaction-title"
        class="mt-xxs text-[24px] font-semibold leading-[1.2] tracking-[-0.48px] text-black"
      >
        위험 거래 <span class="text-error">{{ count }}건</span> 발생
      </h2>
      <button
        class="mt-xs inline-flex min-h-touch-target items-center gap-xxs text-[20px] font-medium leading-[1.2] tracking-[-0.4px] text-gray-500"
        type="button"
        @click="emit('confirm')"
      >
        거래 확인하기
        <ChevronRight class="size-6 text-gray-500" aria-hidden="true" />
      </button>
    </div>

    <button
      class="flex size-touch-target shrink-0 items-center justify-center text-gray-500"
      type="button"
      aria-label="위험 거래 안내 닫기"
      @click="emit('close')"
    >
      <X class="size-6" aria-hidden="true" />
    </button>
  </section>
</template>
