<script setup lang="ts">
import { ChevronRight } from '@lucide/vue'

import type { TransferRecipient } from '@/stores/transfer.store'

defineProps<{
  recipient: TransferRecipient
}>()

const emit = defineEmits<{
  select: [recipient: TransferRecipient]
}>()
</script>

<template>
  <button
    class="flex w-full items-center gap-md border-b border-border px-md py-md text-left last:border-b-0"
    type="button"
    @click="emit('select', recipient)"
  >
    <span class="min-w-0 flex-1">
      <strong class="type-h2 block">{{ recipient.name }}</strong>
      <span class="type-body-medium block truncate text-body-muted">
        {{ recipient.bank }} {{ recipient.accountNumber }}
      </span>
    </span>
    <ChevronRight
      class="size-xl shrink-0 text-body-muted"
      :stroke-width="2.5"
      aria-hidden="true"
    />
  </button>
</template>
