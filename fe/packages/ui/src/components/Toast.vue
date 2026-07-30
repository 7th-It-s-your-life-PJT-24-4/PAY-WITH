<script setup lang="ts">
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
  }>(),
  {
    duration: 2200,
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
      class="fixed left-1/2 z-[70] -translate-x-1/2 rounded-[10px] bg-[#3f434d] px-md py-[14px] text-[15px] font-medium leading-[22.5px] tracking-[-0.3px] text-white shadow-[0_0_6px_rgba(37,38,44,0.1)]"
      :style="{ bottom: 'calc(63px + env(safe-area-inset-bottom))' }"
      role="status"
      aria-live="polite"
      @update:open="emit('update:open', $event)"
    >
      <ToastDescription>{{ message }}</ToastDescription>
    </ToastRoot>
    <ToastViewport class="fixed inset-x-0 bottom-0 z-[70]" />
  </ToastProvider>
</template>
