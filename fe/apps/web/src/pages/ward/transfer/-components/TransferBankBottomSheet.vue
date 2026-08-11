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

import type { Bank } from '@/schemas/bank.schema'
import { getBankPresentation } from '@/utils/bank-presentation'

defineProps<{
  open: boolean
  banks: Bank[]
  selectedBankCode?: string
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
  select: [bank: Bank]
}>()

function handleSelect(bank: Bank) {
  emit('select', bank)
  emit('update:open', false)
}
</script>

<template>
  <DialogRoot :open="open" @update:open="emit('update:open', $event)">
    <DialogPortal>
      <DialogOverlay
        class="fixed inset-0 z-50 bg-overlay/40 backdrop-blur-[2px]"
      />
      <DialogContent
        class="fixed bottom-0 left-1/2 z-50 flex max-h-[85dvh] w-full max-w-[390px] -translate-x-1/2 flex-col rounded-t-[28px] bg-surface-card px-mobile-gutter pb-[calc(var(--spacing-xl)+env(safe-area-inset-bottom))] pt-sm shadow-modal focus:outline-none"
        @close-auto-focus="$event.preventDefault()"
      >
        <div
          class="mx-auto h-1.5 w-12 rounded-full bg-border-strong"
          aria-hidden="true"
        />

        <div
          class="mt-sm flex min-h-touch-target items-center justify-between gap-md"
        >
          <div>
            <DialogTitle class="type-h2 text-body">은행 선택</DialogTitle>
            <DialogDescription class="sr-only">
              송금할 은행을 선택해주세요
            </DialogDescription>
          </div>
          <DialogClose
            class="flex size-12 shrink-0 items-center justify-center rounded-full text-body-secondary outline-none hover:bg-disabled/60 focus-visible:ring-2 focus-visible:ring-focus"
            aria-label="은행 선택 닫기"
          >
            <X class="size-xl" aria-hidden="true" />
          </DialogClose>
        </div>

        <div
          class="mt-md min-h-0 flex-1 overflow-y-auto pr-1"
          role="region"
          aria-label="전체 은행 목록"
        >
          <div class="grid grid-cols-3 gap-sm pb-md">
            <button
              v-for="bank in banks"
              :key="bank.bankCode"
              class="flex min-h-[92px] flex-col items-center justify-center gap-xs rounded-medium border p-xs text-center transition-all focus-visible:ring-2 focus-visible:ring-focus"
              :class="
                selectedBankCode === bank.bankCode
                  ? 'border-primary-500 bg-primary-900 text-primary-500 font-bold shadow-sm ring-1 ring-primary-500'
                  : 'border-border bg-surface-card text-body hover:border-border-strong active:bg-disabled/30'
              "
              type="button"
              :aria-label="bank.bankName"
              @click="handleSelect(bank)"
            >
              <span
                :class="[
                  'flex size-10 items-center justify-center overflow-hidden rounded-full shadow-xs',
                  getBankPresentation(bank).brandClass,
                ]"
                aria-hidden="true"
              >
                <img
                  v-if="getBankPresentation(bank).iconUrl"
                  class="size-6 object-contain"
                  :src="getBankPresentation(bank).iconUrl"
                  alt=""
                />
                <span v-else class="type-caption font-bold text-white">
                  {{ bank.bankName.slice(0, 1) }}
                </span>
              </span>
              <span class="type-caption line-clamp-1 break-all">
                {{ bank.bankName }}
              </span>
            </button>
          </div>
        </div>
      </DialogContent>
    </DialogPortal>
  </DialogRoot>
</template>
