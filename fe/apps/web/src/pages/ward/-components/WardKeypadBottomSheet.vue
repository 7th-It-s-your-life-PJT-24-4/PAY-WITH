<script setup lang="ts">
import { X } from '@lucide/vue'
import {
  DialogClose,
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle,
} from 'reka-ui'

withDefaults(
  defineProps<{
    open: boolean
    title: string
    description?: string
    closeOnOutside?: boolean
    mode?: 'modal' | 'input'
  }>(),
  {
    description: undefined,
    closeOnOutside: true,
    mode: 'modal',
  },
)

const emit = defineEmits<{
  'update:open': [value: boolean]
}>()
</script>

<template>
  <DialogRoot
    :open="open"
    :modal="mode === 'modal'"
    @update:open="emit('update:open', $event)"
  >
    <DialogPortal>
      <DialogOverlay
        v-if="mode === 'modal'"
        class="fixed inset-0 z-50 bg-overlay/40 backdrop-blur-[2px]"
      />
      <DialogContent
        class="fixed bottom-0 left-1/2 z-50 flex max-h-[calc(100dvh-24px)] w-full max-w-[390px] -translate-x-1/2 flex-col rounded-t-[28px] bg-surface-card px-mobile-gutter pb-[calc(var(--spacing-xl)+env(safe-area-inset-bottom))] pt-sm shadow-modal focus:outline-none"
        @open-auto-focus="
          mode === 'input' ? $event.preventDefault() : undefined
        "
        @close-auto-focus="$event.preventDefault()"
        @pointer-down-outside="
          closeOnOutside ? undefined : $event.preventDefault()
        "
      >
        <div
          class="mx-auto h-1.5 w-12 rounded-full bg-border-strong"
          aria-hidden="true"
        />

        <div class="mt-sm flex min-h-touch-target items-center gap-md">
          <div class="min-w-0 flex-1">
            <DialogTitle class="type-h2 text-body">
              {{ title }}
            </DialogTitle>
            <DialogDescription
              v-if="description"
              class="type-body-medium mt-xxs text-body-secondary"
            >
              {{ description }}
            </DialogDescription>
            <DialogDescription v-else class="sr-only">
              {{ title }} 바텀시트
            </DialogDescription>
          </div>
          <DialogClose
            class="flex size-12 shrink-0 items-center justify-center rounded-full text-body-secondary outline-none hover:bg-disabled/60 focus-visible:ring-2 focus-visible:ring-focus"
            aria-label="바텀시트 닫기"
          >
            <X class="size-xl" aria-hidden="true" />
          </DialogClose>
        </div>

        <div class="mt-lg min-h-0 overflow-y-auto">
          <slot />
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
