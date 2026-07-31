<script setup lang="ts">
import {
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle,
} from 'reka-ui'
import { X } from '@lucide/vue'

withDefaults(
  defineProps<{
    title: string
    description?: string
    closeLabel?: string
    contentClass?: string
  }>(),
  {
    description: '',
    closeLabel: '닫기',
    contentClass: '',
  },
)

const open = defineModel<boolean>('open', { default: false })
</script>

<template>
  <DialogRoot v-model:open="open">
    <DialogPortal>
      <DialogOverlay
        class="fixed inset-0 z-40 bg-black/25 transition-opacity duration-300 data-[state=closed]:opacity-0 data-[state=open]:opacity-100"
      />
      <DialogContent
        :class="[
          'fixed inset-x-0 bottom-0 z-50 mx-auto max-h-[85dvh] w-full max-w-[390px] overflow-hidden rounded-t-[20px] bg-white px-5 pb-[calc(20px+env(safe-area-inset-bottom))] pt-12 shadow-[0_-8px_24px_rgba(0,0,0,0.08)] outline-none transition-transform duration-300 data-[state=closed]:translate-y-full data-[state=open]:translate-y-0',
          contentClass,
        ]"
      >
        <DialogClose
          class="absolute right-3 top-3 flex size-7 items-center justify-center rounded-full bg-[#F0F3F8] text-gray-600"
          type="button"
          :aria-label="closeLabel"
        >
          <X class="size-5" aria-hidden="true" />
        </DialogClose>

        <DialogTitle
          class="text-[20px] font-bold leading-[1.2] tracking-[-0.4px] text-black"
        >
          {{ title }}
        </DialogTitle>
        <DialogDescription v-if="description" class="sr-only">
          {{ description }}
        </DialogDescription>

        <slot />
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
