<script setup lang="ts">
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
    <span
      class="type-h4 flex size-14 shrink-0 items-center justify-center rounded-full bg-primary-900 text-primary-300"
      aria-hidden="true"
    >
      {{ recipient.name.slice(0, 1) }}
    </span>
    <span class="min-w-0 flex-1">
      <strong class="type-h2 block">{{ recipient.name }}</strong>
      <span class="type-body-medium block truncate text-body-muted">
        {{ recipient.bank }} {{ recipient.accountNumber }}
      </span>
    </span>
    <span class="type-h2 text-body-muted" aria-hidden="true">›</span>
  </button>
</template>
