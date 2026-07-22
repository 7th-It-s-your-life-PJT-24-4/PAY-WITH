<script setup lang="ts">
import {
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle,
} from 'reka-ui'
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    open: boolean
    title: string
    description?: string
    size?: 'default' | 'large'
    iconTone?: 'primary' | 'success' | 'error'
    closeOnOutside?: boolean
    closeOnEscape?: boolean
  }>(),
  {
    description: undefined,
    size: 'default',
    iconTone: 'primary',
    closeOnOutside: true,
    closeOnEscape: true,
  },
)

const emit = defineEmits<{
  'update:open': [value: boolean]
}>()

const iconToneClass = computed(
  () =>
    ({
      primary: 'bg-primary-900 text-primary-500',
      success: 'bg-success/10 text-success',
      error: 'bg-error/10 text-error',
    })[props.iconTone],
)

const actionButtonSize = computed(() =>
  props.size === 'large' ? 'large' : 'default',
)

let blockCurrentDismiss = false

function close() {
  emit('update:open', false)
}

function handlePointerDownOutside(event: Event) {
  if (!props.closeOnOutside) preventCurrentDismiss(event)
}

function handleEscapeKeyDown(event: KeyboardEvent) {
  if (!props.closeOnEscape) preventCurrentDismiss(event)
}

function preventCurrentDismiss(event: Event) {
  blockCurrentDismiss = true
  event.preventDefault()
  queueMicrotask(() => {
    blockCurrentDismiss = false
  })
}

function handleOpenChange(value: boolean) {
  if (!value && blockCurrentDismiss) return
  emit('update:open', value)
}
</script>

<template>
  <DialogRoot :open="open" @update:open="handleOpenChange">
    <DialogPortal>
      <DialogOverlay
        class="fixed inset-0 z-50 bg-overlay/40 backdrop-blur-[2px]"
      />
      <DialogContent
        class="fixed left-1/2 top-1/2 z-50 flex max-h-[calc(100dvh-40px)] w-[calc(100%-40px)] max-w-[316px] -translate-x-1/2 -translate-y-1/2 flex-col overflow-y-auto rounded-large bg-surface-card p-xl text-center shadow-modal focus:outline-none"
        @pointer-down-outside="handlePointerDownOutside"
        @escape-key-down="handleEscapeKeyDown"
      >
        <div
          v-if="$slots.icon"
          class="mx-auto flex shrink-0 items-center justify-center rounded-full"
          :class="[
            iconToneClass,
            size === 'large' ? 'size-[76px]' : 'size-[64px]',
          ]"
          aria-hidden="true"
        >
          <slot name="icon" />
        </div>

        <DialogTitle
          class="text-body"
          :class="[
            size === 'large' ? 'type-h1 mt-xs' : 'type-h2 mt-xs',
            !$slots.icon ? 'mt-0' : '',
          ]"
        >
          {{ title }}
        </DialogTitle>

        <DialogDescription
          v-if="description"
          class="mt-lg whitespace-pre-line text-body-muted"
          :class="size === 'large' ? 'type-h3' : 'type-body-medium'"
        >
          {{ description }}
        </DialogDescription>
        <DialogDescription v-else class="sr-only">
          {{ title }} 대화상자
        </DialogDescription>

        <div v-if="$slots.default" class="mt-xl">
          <slot />
        </div>

        <div v-if="$slots.actions" class="mt-xl">
          <slot name="actions" :close="close" :button-size="actionButtonSize" />
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
