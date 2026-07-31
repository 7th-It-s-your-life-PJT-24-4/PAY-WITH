<script setup lang="ts">
import {
  DialogContent,
  DialogDescription,
  DialogOverlay,
  DialogPortal,
  DialogRoot,
  DialogTitle,
} from 'reka-ui'

import Button from './Button.vue'

withDefaults(
  defineProps<{
    open: boolean
    title: string
    cancelLabel?: string
    confirmLabel?: string
    confirmDisabled?: boolean
  }>(),
  {
    cancelLabel: '취소',
    confirmLabel: '확인',
    confirmDisabled: false,
  },
)

const emit = defineEmits<{
  'update:open': [value: boolean]
  cancel: []
  confirm: []
}>()

function cancel() {
  emit('cancel')
  emit('update:open', false)
}
</script>

<template>
  <DialogRoot :open="open" @update:open="emit('update:open', $event)">
    <DialogPortal>
      <DialogOverlay class="fixed inset-0 z-50 bg-black/25" />
      <DialogContent
        class="fixed left-1/2 top-1/2 z-50 flex w-[298px] -translate-x-1/2 -translate-y-1/2 flex-col items-center gap-lg rounded-large bg-white p-lg text-center shadow-modal focus:outline-none"
        @pointer-down-outside.prevent
        @escape-key-down.prevent
      >
        <DialogTitle
          class="w-full text-center text-[18px] font-bold leading-[26px] tracking-[-0.2px] text-[#232529]"
        >
          {{ title }}
        </DialogTitle>
        <DialogDescription class="sr-only">
          {{ title }} 대화상자
        </DialogDescription>

        <div class="flex w-[258px] gap-[10px]">
          <Button
            class="!h-[52px] !min-h-0 flex-1 basis-0 !rounded-[12px] !border-0 !bg-[#f0f3f8] !px-md !py-[14px] !text-[16px] !font-bold !leading-6 !tracking-[-0.2px] !text-[#3b3e43]"
            :label="cancelLabel"
            variant="secondary"
            size="small"
            @click="cancel"
          />
          <Button
            class="!h-[52px] !min-h-0 flex-1 basis-0 !rounded-[12px] !border-0 !bg-primary-500 !px-md !py-[10px] !text-[16px] !font-bold !leading-6 !tracking-[-0.2px] !text-white"
            :label="confirmLabel"
            :disabled="confirmDisabled"
            size="small"
            @click="emit('confirm')"
          />
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
