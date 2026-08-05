<script setup lang="ts">
import { Check, ChevronLeft, Pencil } from '@lucide/vue'

withDefaults(
  defineProps<{
    title: string
    showBack?: boolean
    action?: 'edit' | 'save'
    actionDisabled?: boolean
  }>(),
  {
    showBack: false,
    action: undefined,
    actionDisabled: false,
  },
)

const emit = defineEmits<{
  back: []
  action: []
}>()
</script>

<template>
  <header class="grid h-11 grid-cols-[44px_1fr_44px] items-center bg-white">
    <button
      v-if="showBack"
      class="flex size-11 items-center justify-center text-[#3b3e43] outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-focus"
      type="button"
      aria-label="뒤로 가기"
      @click="emit('back')"
    >
      <ChevronLeft class="size-6" :stroke-width="1.8" aria-hidden="true" />
    </button>
    <span v-else aria-hidden="true" />

    <h1
      class="text-center text-[20px] font-bold leading-[1.2] tracking-[-0.4px] text-black"
    >
      {{ title }}
    </h1>

    <button
      v-if="action"
      class="flex size-11 items-center justify-center text-black outline-none focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-focus disabled:cursor-not-allowed disabled:text-gray-700"
      type="button"
      :disabled="actionDisabled"
      :aria-label="action === 'edit' ? '내 정보 수정' : '내 정보 저장'"
      @click="emit('action')"
    >
      <Pencil
        v-if="action === 'edit'"
        class="size-5"
        :stroke-width="2"
        aria-hidden="true"
      />
      <Check v-else class="size-6" :stroke-width="2" aria-hidden="true" />
    </button>
    <span v-else aria-hidden="true" />
  </header>
</template>
