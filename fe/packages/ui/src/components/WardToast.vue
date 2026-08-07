<script setup lang="ts">
import { CircleAlert, Info } from '@lucide/vue'
import {
  ToastDescription,
  ToastProvider,
  ToastRoot,
  ToastViewport,
} from 'reka-ui'

withDefaults(
  defineProps<{
    open: boolean
    message: string
    duration?: number
    iconTone?: 'info' | 'warning' | 'error'
  }>(),
  {
    duration: 3000,
    iconTone: 'warning',
  },
)

const emit = defineEmits<{
  'update:open': [value: boolean]
}>()
</script>

<template>
  <ToastProvider>
    <ToastRoot
      :open="open"
      :duration="duration"
      class="fixed left-1/2 z-[70] flex max-w-[90vw] -translate-x-1/2 items-center gap-sm rounded-full border border-gray-300/60 bg-gray-100 px-lg py-md text-[18px] font-semibold leading-tight text-gray-900 shadow-2xl"
      :style="{ bottom: 'calc(96px + env(safe-area-inset-bottom))' }"
      role="status"
      aria-live="polite"
      @update:open="emit('update:open', $event)"
    >
      <Info
        v-if="iconTone === 'info'"
        class="size-xl shrink-0 text-primary-400"
        :stroke-width="2.5"
        aria-hidden="true"
      />
      <CircleAlert
        v-else-if="iconTone === 'error'"
        class="size-xl shrink-0 text-red-400"
        :stroke-width="2.5"
        aria-hidden="true"
      />
      <CircleAlert
        v-else
        class="size-xl shrink-0 text-amber-400"
        :stroke-width="2.5"
        aria-hidden="true"
      />
      <ToastDescription class="truncate">{{ message }}</ToastDescription>
    </ToastRoot>
    <ToastViewport class="fixed inset-x-0 bottom-0 z-[70]" />
  </ToastProvider>
</template>
